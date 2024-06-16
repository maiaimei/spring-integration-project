package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpOutboundRule;
import cn.maiaimei.spring.integration.sftp.config.rule.SimpleSftpOutboundRule;
import cn.maiaimei.spring.integration.sftp.constants.SftpConstants;
import cn.maiaimei.spring.integration.sftp.handler.advice.CustomRequestHandlerRetryAdvice;
import java.io.File;
import org.aopalliance.aop.Advice;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * SFTP outbound factory
 */
public class SftpOutboundFactory extends BaseSftpFactory {

  private static final String RETRY_MAX_ATTEMPTS = "sftp.outbound.retry.maxAttempts";
  private static final String RETRY_MAX_WAIT_TIME = "sftp.outbound.retry.maxWaitTime";

  /**
   * Construct a {@link IntegrationFlow} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link IntegrationFlow} instance
   */
  public IntegrationFlow createSimpleSftpOutboundFlow(SimpleSftpOutboundRule rule) {
    validateRule(rule);
    log.info("Init sftp outbound rule named {}, id: {}", rule.getName(), rule.getId());
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(rule.getMaxMessagesPerPoll())))
        .wireTap(info("[{}] File {} is detected in local folder", rule))
        .handle(Sftp.outboundGateway(template(rule), Command.PUT, SftpConstants.PAYLOAD))
        .wireTap(info("[{}] File {} has been uploaded to remote folder", rule))
        .handle(moveToSent(rule))
        .get();
  }

  /**
   * Construct a {@link IntegrationFlow} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link IntegrationFlow} instance
   */
  public IntegrationFlow createAdvancedSftpOutboundFlow(SimpleSftpOutboundRule rule) {
    validateRule(rule);
    log.info("Init sftp outbound rule named {}, id: {}", rule.getName(), rule.getId());
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(rule.getMaxMessagesPerPoll())))
        .wireTap(info("[{}] File {} is detected in local folder", rule))
        .handle(new SftpOutboundGateway(template(rule), Command.PUT.getCommand(), SftpConstants.PAYLOAD),
            e -> e.advice(uploadFileAdvice(rule)))
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
        String targetFolder = rule.getArchive();
        String targetFolderName = SftpConstants.ARCHIVE;
        if (SftpConstants.FAILED.equals(
            requestMessage.getHeaders().get(SftpConstants.PROCESS_STATUS))) {
          targetFolder = FileUtils.normalizePath(
              rule.getArchive() + File.separator + SftpConstants.ERROR);
          targetFolderName = SftpConstants.ERROR;
        }
        String srcFile = FileUtils.getFilePath(rule.getLocal(), fileName);
        String destFile = FileUtils.getFilePath(targetFolder, fileName);
        FileUtils.moveFile(srcFile, destFile);
        log.info("[{}] File {} has been moved to {} folder", rule.getName(), fileName, targetFolderName);
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
    Assert.hasText(rule.getId(), "id must be configured");
    Assert.hasText(rule.getName(), "name must be configured");
    Assert.hasText(rule.getSchema(), "schema must be configured");
    Assert.hasText(rule.getPattern(), "pattern must be configured");
    Assert.hasText(rule.getLocal(), "local must be configured");
    Assert.hasText(rule.getRemote(), "remote must be configured");
    Assert.hasText(rule.getArchive(), "archive must be configured");
  }

  private IntegrationFlow info(String format, BaseSftpOutboundRule rule) {
    return flow -> flow.handle(
        message -> log.info(format, rule.getName(), message.getHeaders().get(FileHeaders.FILENAME))
    );
  }


  /**
   * Construct a {@link Advice} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link Advice} instance
   */
  private Advice uploadFileAdvice(BaseSftpOutboundRule rule) {
    CustomRequestHandlerRetryAdvice advice = new CustomRequestHandlerRetryAdvice(applicationContext);
    advice.setRuleName(rule.getName());
    advice.setRetryMaxAttempts(rule.getRetryMaxAttempts(), RETRY_MAX_ATTEMPTS);
    advice.setRetryMaxWaitTime(rule.getRetryMaxWaitTime(), RETRY_MAX_WAIT_TIME);
    advice.setFileNameFunction(message -> (String) message.getHeaders().get(FileHeaders.FILENAME));
    advice.setLogDescription("upload to remote folder");
    advice.afterPropertiesSet();
    return advice;
  }

}
