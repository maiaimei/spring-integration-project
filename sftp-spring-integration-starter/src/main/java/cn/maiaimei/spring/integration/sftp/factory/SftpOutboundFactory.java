package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpOutboundRule;
import cn.maiaimei.spring.integration.sftp.config.rule.SimpleSftpOutboundRule;
import cn.maiaimei.spring.integration.sftp.constants.SftpConstants;
import java.io.File;
import java.util.Objects;
import org.aopalliance.aop.Advice;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * SFTP outbound factory
 */
public class SftpOutboundFactory extends BaseSftpFactory {

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
            e -> e.advice(sftpOutboundGatewayAdvice(rule)))
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
        String targetFolderName = "archive";
        if (SftpConstants.FAILED.equals(
            requestMessage.getHeaders().get(SftpConstants.SEND_STATUS))) {
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
  private Advice sftpOutboundGatewayAdvice(BaseSftpOutboundRule rule) {
    int maxAttempts = getMaxRetries(rule.getMaxRetries());
    long backOffPeriod = getMaxRetries(rule.getMaxRetryWaitTime());
    final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(maxAttempts);
    final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(backOffPeriod);
    final RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    final RequestHandlerRetryAdvice advice = new CustomRequestHandlerRetryAdvice(rule, maxAttempts,
        backOffPeriod);
    advice.setRetryTemplate(retryTemplate);
    retryTemplate.registerListener(advice);
    return advice;
  }

  /**
   * Get the maximum number of retry attempts
   *
   * @return the maximum number of retry attempts
   */
  private Integer getMaxRetries(int maxRetries) {
    if (maxRetries > 0) {
      return maxRetries;
    }
    return applicationContext.getEnvironment()
        .getProperty("sftp.outbound.retry.maxAttempts", Integer.class);
  }

  /**
   * Get the maximum retry wait time in milliseconds
   *
   * @return the maximum retry wait time in milliseconds
   */
  private Long getMaxRetries(long maxRetryWaitTime) {
    if (maxRetryWaitTime > 0) {
      return maxRetryWaitTime;
    }
    return applicationContext.getEnvironment()
        .getProperty("sftp.outbound.retry.waitTime", Long.class);
  }

  private static class CustomRequestHandlerRetryAdvice extends RequestHandlerRetryAdvice {

    private final Logger log;
    private final BaseSftpOutboundRule rule;
    private final int maxAttempts;
    private final long backOffPeriod;

    public CustomRequestHandlerRetryAdvice(BaseSftpOutboundRule rule, int maxAttempts,
        long backOffPeriod) {
      this.log = LoggerFactory.getLogger(CustomRequestHandlerRetryAdvice.class);
      this.rule = rule;
      this.maxAttempts = maxAttempts;
      this.backOffPeriod = backOffPeriod;
    }

    @Override
    protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
      log.info("The maximum number of retry attempts including the initial attempt is {}, wait "
          + "time in milliseconds is {}", maxAttempts, backOffPeriod);
      String fileName = (String) message.getHeaders().get(FileHeaders.FILENAME);
      String sentStatus = SftpConstants.SUCCESS;
      try {
        super.doInvoke(callback, target, message);
        log.info("[{}] File {} has been uploaded to remote folder", rule.getName(), fileName);
      } catch (Exception e) {
        sentStatus = SftpConstants.FAILED;
        log.error(
            String.format("[%s] File %s failed to upload to remote folder after %s retry attempts",
                rule.getName(), fileName, maxAttempts), e);
      }
      return MessageBuilder.withPayload(message.getPayload())
          .copyHeaders(message.getHeaders())
          .setHeaderIfAbsent(SftpConstants.SEND_STATUS, sentStatus)
          .build();
    }

    @Override
    public <T, E extends Throwable> void onSuccess(RetryContext context,
        RetryCallback<T, E> callback, T result) {
      final Object message = context.getAttribute(SftpConstants.MESSAGE);
      final int retryCount = context.getRetryCount();
      if (Objects.nonNull(message) && retryCount > 0) {
        Message<?> requestMessage = (Message<?>) message;
        final String fileName = (String) requestMessage.getHeaders().get(FileHeaders.FILENAME);
        log.info("[{}] File {} has been uploaded to remote folder for the {} time",
            rule.getName(), fileName, retryCount);
      }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
        RetryCallback<T, E> callback, Throwable throwable) {
      final Object message = context.getAttribute(SftpConstants.MESSAGE);
      final int retryCount = context.getRetryCount();
      if (Objects.nonNull(message) && retryCount > 0) {
        Message<?> requestMessage = (Message<?>) message;
        final String fileName = (String) requestMessage.getHeaders().get(FileHeaders.FILENAME);
        log.error(String.format("[%s] File %s failed to upload to remote folder for the %s time",
                rule.getName(), fileName, retryCount),
            throwable);
      }
    }

  }

}
