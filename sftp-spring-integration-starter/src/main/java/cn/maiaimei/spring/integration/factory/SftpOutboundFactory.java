package cn.maiaimei.spring.integration.factory;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.commons.lang.utils.StringUtils;
import cn.maiaimei.spring.integration.config.rule.BaseSftpOutboundRule;
import cn.maiaimei.spring.integration.config.rule.SimpleSftpOutboundRule;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * SFTP outbound factory
 */
@Slf4j
@Component
public class SftpOutboundFactory implements ApplicationContextAware {

  private static final String PAYLOAD = "payload";
  private static final String SEND_STATUS = "sendStatus";

  private ApplicationContext applicationContext;

  @Autowired
  private Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Construct a {@link IntegrationFlow} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link IntegrationFlow} instance
   */
  public IntegrationFlow createSimpleSftpOutboundFlow(SimpleSftpOutboundRule rule) {
    validateRule(rule);
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(rule.getMaxMessagesPerPoll())))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} is detected in {}",
                rule.getName(), message.getHeaders().get(FileHeaders.FILENAME), rule.getLocal())
        ))
        .handle(Sftp.outboundGateway(template(rule), Command.PUT, PAYLOAD))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} has been uploaded to {}",
                rule.getName(), message.getHeaders().get(FileHeaders.FILENAME), rule.getRemote())
        ))
        .handle(message -> MessageBuilder.fromMessage(message)
            .setHeaderIfAbsent(SEND_STATUS, SendStatus.SECCESS.name()))
        .handle(moveToSent(rule))
        .get();
  }

  /**
   * Construct a {@link FileReadingMessageSource} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link FileReadingMessageSource} instance
   */
  private FileReadingMessageSource fileReadingMessageSource(BaseSftpOutboundRule rule) {
    CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new SimplePatternFileListFilter(rule.getPattern()));
    if (rule.isAcceptOnce()) {
      filter.addFilter(new AcceptOnceFileListFilter<>());
    }

    FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setDirectory(FileUtils.getFile(rule.getLocal()));
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setFilter(filter);
    return messageSource;
  }

  /**
   * Move the file from local to sent or send depending on status.
   *
   * @param rule the rule to use
   * @return a {@link AbstractReplyProducingMessageHandler} instance
   */
  private AbstractReplyProducingMessageHandler moveToSent(BaseSftpOutboundRule rule) {
    return new AbstractReplyProducingMessageHandler() {
      @Override
      protected Object handleRequestMessage(Message<?> requestMessage) {
        final String fileName = (String) requestMessage.getHeaders().get(FileHeaders.FILENAME);
        final String sendStatus = (String) requestMessage.getHeaders().get(SEND_STATUS);
        String postSendDestination = rule.getSent();
        if (!StringUtils.hasText(sendStatus) || SendStatus.FAILED.name().equals(sendStatus)) {
          postSendDestination = rule.getSend();
        }
        String srcFile = FileUtils.getFilePath(rule.getLocal(), fileName);
        String destFile = FileUtils.getFilePath(postSendDestination, fileName);
        FileUtils.moveFile(srcFile, destFile);
        log.info("[{}] File {} has been moved from {} to {}",
            rule.getName(), fileName, rule.getLocal(), postSendDestination);
        // return null to terminate the flow
        return null;
      }
    };
  }

  /**
   * Construct a {@link RemoteFileTemplate} instance by the given rule.
   *
   * @param rule the rule to construct instance
   * @return a {@link RemoteFileTemplate} instance
   */
  private RemoteFileTemplate<DirEntry> template(BaseSftpOutboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(
        sessionFactoryMap.get(rule.getSchema()));
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getRemote()));
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setUseTemporaryFileName(Boolean.TRUE);
    template.setBeanFactory(applicationContext);
    // must invoke method "afterPropertiesSet", 
    // otherwise will throw java.lang.RuntimeException: No beanFactory
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Validate the given rule
   *
   * @param rule the rule to validate
   */
  private void validateRule(BaseSftpOutboundRule rule) {
    Assert.hasText(rule.getSchema(), "schema must be configured");
    Assert.hasText(rule.getName(), "name must be configured");
    Assert.hasText(rule.getPattern(), "pattern must be configured");
    Assert.hasText(rule.getLocal(), "local must be configured");
    Assert.hasText(rule.getRemote(), "remote must be configured");
    Assert.hasText(rule.getSent(), "sent must be configured");
  }

  private enum SendStatus {
    SECCESS,
    FAILED
  }

}
