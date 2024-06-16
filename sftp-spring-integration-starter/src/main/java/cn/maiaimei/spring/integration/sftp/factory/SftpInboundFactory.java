package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.constants.DateTimeConstants;
import cn.maiaimei.commons.lang.utils.DateTimeUtils;
import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.commons.lang.utils.StringUtils;
import cn.maiaimei.commons.lang.utils.ValueExpressionUtils;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import cn.maiaimei.spring.integration.sftp.constants.SftpConstants;
import cn.maiaimei.spring.integration.sftp.handler.advice.CustomRequestHandlerRetryAdvice;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.aopalliance.aop.Advice;
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
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * SftpInboundFactory
 */
public class SftpInboundFactory extends BaseSftpFactory {

  private static final String RETRY_MAX_ATTEMPTS = "sftp.inbound.retry.maxAttempts";
  private static final String RETRY_MAX_WAIT_TIME = "sftp.inbound.retry.maxWaitTime";

  private static final String SOURCE_FILE_EXPRESSION_FORMAT = "'%s/' + headers['file_remoteFile']";
  private static final String TEMP_FILE_EXPRESSION_FORMAT = "'%s/' + headers['file_remoteFile']";
  private static final String ARCHIVE_BY_DATE_FILE_EXPRESSION_FORMAT = "'%s/' + headers['now'] + "
      + "'/' + headers['file_remoteFile']";
  private static final String ARCHIVE_FILE_EXPRESSION_FORMAT = "'%s/' + headers['file_remoteFile']";

  /**
   * Construct a {@link IntegrationFlow} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link IntegrationFlow} instance
   */
  public IntegrationFlow createSimpleSftpInboundFlow(BaseSftpInboundRule rule) {
    validateRule(rule);
    log.info("Init sftp inbound rule named {}, id: {}", rule.getName(), rule.getId());
    final AtomicInteger counter = new AtomicInteger();
    String sourceFileExpression = getSourceFileExpression(rule);
    String tempFileExpression = getTempFileExpression(rule);
    String archiveFileExpression = getArchiveFileExpression(rule);
    return IntegrationFlow.from(sftpStreamingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron())
                .maxMessagesPerPoll(rule.getMaxMessagesPerPoll())
                .errorHandler(sftpStreamingMessageSourceErrorHandler(rule))
            ))
        .handle(closeSession(rule))
        .wireTap(info("[{}] File {} is detected in remote folder", rule))
        // https://docs.spring.io/spring-integration/reference/sftp/outbound-gateway.html#using-the-mv-command
        .handle(Sftp.outboundGateway(template(rule), Command.MV, sourceFileExpression).renameExpression(tempFileExpression))
        .wireTap(info("[{}] File {} has been moved to temp folder", rule))
        .handle(getStream(rule, tempFileExpression), e -> e.advice(getStreamAdvice(rule)))
        .handle(checkStream(rule))
        .handle(download(rule, counter))
        .handle(closeSession(rule))
        .wireTap(info("[{}] File {} has been downloaded to local folder", rule))
        .enrichHeaders(h -> {
          if (rule.isArchiveByDate()) {
            h.header("now", DateTimeUtils.formatNow(DateTimeConstants.YYYYMMDD));
          }
        })
        .handle(Sftp.outboundGateway(template(rule), Command.MV, tempFileExpression).renameExpression(archiveFileExpression))
        .wireTap(info("[{}] File {} has been moved to archive folder", rule))
        .channel("nullChannel")
        .get();
  }

  private String getSourceFileExpression(BaseSftpInboundRule rule) {
    return String.format(SOURCE_FILE_EXPRESSION_FORMAT, rule.getRemoteSource());
  }

  private String getTempFileExpression(BaseSftpInboundRule rule) {
    return String.format(TEMP_FILE_EXPRESSION_FORMAT, rule.getRemoteTemp());
  }

  private String getArchiveFileExpression(BaseSftpInboundRule rule) {
    String archiveFileExpression;
    if (rule.isArchiveByDate()) {
      archiveFileExpression = String.format(ARCHIVE_BY_DATE_FILE_EXPRESSION_FORMAT,
          rule.getRemoteArchive());
    } else {
      archiveFileExpression = String.format(ARCHIVE_FILE_EXPRESSION_FORMAT,
          rule.getRemoteArchive());
    }
    return archiveFileExpression;
  }

  /**
   * Produces message with payloads of type InputStream, letting you fetch files without writing to the local file system.
   * <p>
   * Since the session remains open, the consuming application is responsible for closing the session when the file has been
   * consumed.
   * <p>
   * The session is provided in the closeableResource header (IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE).
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
    messageSource.setMaxFetchSize(rule.getMaxFetchSize());
    return messageSource;
  }

  private ErrorHandler sftpStreamingMessageSourceErrorHandler(BaseSftpInboundRule rule) {
    return new ErrorHandler() {
      @Override
      public void handleError(Throwable t) {
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
      }
    };
  }

  private AbstractReplyProducingMessageHandler checkStream(BaseSftpInboundRule rule) {
    return new AbstractReplyProducingMessageHandler() {
      @Override
      protected Object handleRequestMessage(Message<?> requestMessage) {
        if (SftpConstants.FAILED.equals(requestMessage.getHeaders().get(SftpConstants.PROCESS_STATUS))) {
          closeSession(rule, requestMessage);
          return null;
        }
        return requestMessage;
      }
    };
  }

  /**
   * Download file
   *
   * @param rule the rule to use
   * @return an {@link MessageHandler} instance
   */
  private MessageHandler download(BaseSftpInboundRule rule, AtomicInteger counter) {
    final FileWritingMessageHandler handler = new FileWritingMessageHandler(
        FileUtils.getOrCreateDirectory(rule.getLocal()));
    handler.setFileNameGenerator(
        message -> getDownloadFileName(message, rule.getRenameExpression(), counter));
    handler.setFileExistsMode(FileExistsMode.REPLACE);
    return handler;
  }

  /**
   * get download filename
   *
   * @param message          the message to use
   * @param renameExpression the renameExpression to use
   * @param counter          the counter to use
   * @return download filename
   */
  private String getDownloadFileName(Message<?> message, String renameExpression,
      AtomicInteger counter) {
    if (StringUtils.hasText(renameExpression)) {
      final Map<String, String> headerMap = message.getHeaders().entrySet().stream()
          .collect(Collectors.toMap(Entry::getKey, e -> String.valueOf(e.getValue())));
      return ValueExpressionUtils.parse(renameExpression, headerMap, counter);
    } else {
      return (String) message.getHeaders().get(FileHeaders.REMOTE_FILE);
    }
  }

  /**
   * When consuming remote files as streams, you are responsible for closing the Session after the stream is consumed.
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
   * When consuming remote files as streams, you are responsible for closing the Session after the stream is consumed.
   * <p>
   * refer to https://docs.spring.io/spring-integration/reference/sftp/streaming.html
   *
   * @param rule the rule to use
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
    template.setAutoCreateDirectory(Boolean.TRUE);
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
    Assert.hasText(rule.getId(), "id must be configured");
    Assert.hasText(rule.getName(), "name must be configured");
    Assert.hasText(rule.getSchema(), "schema must be configured");
    Assert.hasText(rule.getPattern(), "pattern must be configured");
    Assert.hasText(rule.getLocal(), "local must be configured");
    Assert.hasText(rule.getRemoteSource(), "remoteSource must be configured");
    Assert.hasText(rule.getRemoteTemp(), "remoteTemp must be configured");
    Assert.hasText(rule.getRemoteArchive(), "remoteArchive must be configured");
  }

  private IntegrationFlow info(String format, BaseSftpInboundRule rule) {
    return flow -> flow.handle(
        message -> log.info(format, rule.getName(), message.getHeaders().get(FileHeaders.REMOTE_FILE))
    );
  }

  /**
   * Construct a {@link SftpOutboundGateway} instance by the given rule and tempFileExpression.
   *
   * @param rule               the rule to use
   * @param tempFileExpression the tempFileExpression to use
   * @return a {@link SftpOutboundGateway} instance
   */
  private SftpOutboundGateway getStream(BaseSftpInboundRule rule, String tempFileExpression) {
    final SftpOutboundGateway gateway = new SftpOutboundGateway(template(rule), Command.GET.getCommand(), tempFileExpression);
    gateway.setOption(Option.STREAM);
    return gateway;
  }

  /**
   * Construct a {@link Advice} instance by the given rule.
   *
   * @param rule the rule to use
   * @return a {@link Advice} instance
   */
  private Advice getStreamAdvice(BaseSftpInboundRule rule) {
    CustomRequestHandlerRetryAdvice advice = new CustomRequestHandlerRetryAdvice(applicationContext);
    advice.setRuleName(rule.getName());
    advice.setRetryMaxAttempts(rule.getRetryMaxAttempts(), RETRY_MAX_ATTEMPTS);
    advice.setRetryMaxWaitTime(rule.getRetryMaxWaitTime(), RETRY_MAX_WAIT_TIME);
    advice.setFileNameFunction(message -> (String) message.getHeaders().get(FileHeaders.REMOTE_FILE));
    advice.setLogDescription("download to local folder");
    advice.afterPropertiesSet();
    return advice;
  }

}
