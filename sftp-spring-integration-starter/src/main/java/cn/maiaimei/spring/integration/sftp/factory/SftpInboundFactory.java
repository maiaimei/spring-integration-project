package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Option;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * SftpInboundFactory
 */
public class SftpInboundFactory extends BaseSftpFactory {

  /**
   * Construct a {@link IntegrationFlow} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link IntegrationFlow} instance
   */
  public IntegrationFlow createSimpleSftpInboundFlow(BaseSftpInboundRule rule) {
    final AtomicInteger counter = new AtomicInteger();
    validateRule(rule);
    String sourceFileExpression = String.format("'%s/' + headers['file_remoteFile']",
        rule.getRemoteSource());
    String tempFileExpression = String.format("'%s/' + headers['file_remoteFile']",
        rule.getRemoteTemp());
    String archiveFileExpression = String.format(
        "'%s/' + headers['now'] + headers['file_remoteFile']",
        rule.getRemoteArchive());
    return IntegrationFlow.from(sftpStreamingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron())
                .maxMessagesPerPoll(rule.getMaxMessagesPerPoll())
                .errorHandler(
                    t -> {
                      if (MessagingExceptionWrapper.class.isAssignableFrom(t.getClass())) {
                        final MessagingExceptionWrapper wrapper =
                            (MessagingExceptionWrapper) t;
                        final Throwable cause = wrapper.getCause();
                        log.error(String.format("[%s] Error occurs in fetching file, message: %s",
                            rule.getName(), cause.getMessage()), cause);
                        closeSession(rule, wrapper.getFailedMessage());
                      } else {
                        log.error(String.format("[%s] Error occurs in fetching file, message: %s",
                            rule.getName(), t.getMessage()), t);
                      }
                    })
            ))
        .handle(closeSession(rule))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} is detected in {}",
                rule.getName(), message.getHeaders().get(FileHeaders.REMOTE_FILE),
                rule.getRemoteSource())
        ))
        // https://docs.spring.io/spring-integration/reference/sftp/outbound-gateway.html#using-the-mv-command
        .handle(Sftp.outboundGateway(template(rule), Command.MV, sourceFileExpression)
            .renameExpression(tempFileExpression))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} has been moved to temp folder, from {} to {}",
                rule.getName(), message.getHeaders().get(FileHeaders.REMOTE_FILE),
                rule.getRemoteSource(), rule.getRemoteTemp())
        ))
        .handle(Sftp.outboundGateway(template(rule), Command.GET, tempFileExpression)
            .options(Option.STREAM))
        .handle(download(rule, counter))
        .handle(closeSession(rule))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} has been downloaded to {}",
                rule.getName(), message.getHeaders().get(FileHeaders.REMOTE_FILE),
                rule.getLocal())
        ))
        .handle(Sftp.outboundGateway(template(rule), Command.MV, tempFileExpression)
            .renameExpression(archiveFileExpression))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} has been moved to archive folder, from {} to {}",
                rule.getName(), message.getHeaders().get(FileHeaders.REMOTE_FILE),
                rule.getRemoteTemp(), rule.getRemoteArchive())
        ))
        .get();
  }

  /**
   * Produces message with payloads of type InputStream, letting you fetch files without writing to
   * the local file system.
   * <p>
   * Since the session remains open, the consuming application is responsible for closing the
   * session when the file has been consumed.
   * <p>
   * The session is provided in the closeableResource header
   * (IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE).
   * <p>
   * refer to https://docs.spring.io/spring-integration/reference/sftp/streaming.html
   *
   * @param rule the rule to use
   * @return a {@link MessageSource} instance
   */
  private MessageSource<InputStream> sftpStreamingMessageSource(BaseSftpInboundRule rule) {
    SftpStreamingMessageSource messageSource = new SftpStreamingMessageSource(template(rule));
    messageSource.setRemoteDirectory(rule.getRemoteSource());
    messageSource.setFilter(new SftpSimplePatternFileListFilter(rule.getPattern()));
    messageSource.setMaxFetchSize(10);
    return messageSource;
  }

  private MessageHandler download(BaseSftpInboundRule rule, AtomicInteger counter) {
    final FileWritingMessageHandler handler = new FileWritingMessageHandler(
        FileUtils.getOrCreateDirectory(rule.getLocal()));
    if (StringUtils.hasText(rule.getRenameExpression())) {
      handler.setFileNameGenerator(
          message -> getFileName(message, rule.getRenameExpression(), counter));
    }
    handler.setFileExistsMode(FileExistsMode.REPLACE);
    return handler;
  }

  private String getFileName(Message<?> message, String renameExpression, AtomicInteger counter) {
    final Map<String, String> headerMap = message.getHeaders().entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> String.valueOf(e.getValue())));
    // TODO
    return null;
  }

  /**
   * When consuming remote files as streams, you are responsible for closing the Session after the
   * stream is consumed.
   * <p>
   * refer to https://docs.spring.io/spring-integration/reference/sftp/streaming.html
   *
   * @param rule the rule to use
   * @return an {@link AbstractReplyProducingMessageHandler} instance
   */
  private AbstractReplyProducingMessageHandler closeSession(BaseSftpInboundRule rule) {
    return new AbstractReplyProducingMessageHandler() {
      @Override
      protected Object handleRequestMessage(Message<?> requestMessage) {
        closeSession(rule, requestMessage);
        return requestMessage;
      }
    };
  }

  /**
   * When consuming remote files as streams, you are responsible for closing the Session after the
   * stream is consumed.
   * <p>
   * refer to https://docs.spring.io/spring-integration/reference/sftp/streaming.html
   *
   * @param rule the rule to use
   * @return an {@link AbstractReplyProducingMessageHandler} instance
   */
  private void closeSession(BaseSftpInboundRule rule, Message<?> requestMessage) {
    if (Objects.nonNull(requestMessage)) {
      final Closeable resource = StaticMessageHeaderAccessor.getCloseableResource(
          requestMessage);
      if (Objects.nonNull(resource)) {
        try {
          resource.close();
        } catch (IOException e) {
          log.error(String.format("[%s] Error occurs in closing session, message: %s",
              rule.getName(), e.getMessage()), e);
        }
      }
    }
  }

  /**
   * Construct a {@link RemoteFileTemplate} instance by the given rule.
   *
   * @param rule the rule to construct instance
   * @return a {@link RemoteFileTemplate} instance
   */
  private RemoteFileTemplate<DirEntry> template(BaseSftpInboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(
        sessionFactoryMap.get(rule.getSchema()));
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
  private void validateRule(BaseSftpInboundRule rule) {
    Assert.hasText(rule.getSchema(), "schema must be configured");
    Assert.hasText(rule.getName(), "name must be configured");
    Assert.hasText(rule.getPattern(), "pattern must be configured");
    Assert.hasText(rule.getLocal(), "local must be configured");
    Assert.hasText(rule.getRemoteSource(), "remoteSource must be configured");
    Assert.hasText(rule.getRemoteTemp(), "remoteSource must be configured");
    Assert.hasText(rule.getRemoteArchive(), "remoteArchive must be configured");
  }
}
