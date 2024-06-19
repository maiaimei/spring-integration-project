package cn.maiaimei.spring.integration.sftp.handler.advice;

import cn.maiaimei.spring.integration.sftp.constants.SftpConstants;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

public class CustomRequestHandlerRetryAdvice extends RequestHandlerRetryAdvice {

  private final Logger log;
  private final ApplicationContext applicationContext;

  private String ruleName;
  private int retryMaxAttempts;
  private long retryMaxWaitTime;
  private String action;
  private String actionCompleted;
  private Function<Message<?>, String> fileNameFunction;

  public CustomRequestHandlerRetryAdvice(ApplicationContext applicationContext) {
    this.log = LoggerFactory.getLogger(CustomRequestHandlerRetryAdvice.class);
    this.applicationContext = applicationContext;
  }

  /**
   * Set the rule name
   */
  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  /**
   * Set the maximum number of retry attempts
   */
  public void setRetryMaxAttempts(int maxAttempts, String configName) {
    if (maxAttempts > 0) {
      this.retryMaxAttempts = maxAttempts;
    } else {
      final Integer configValue = applicationContext.getEnvironment().getProperty(configName, Integer.class);
      if (Objects.isNull(configValue)) {
        throw new IllegalArgumentException("No config named " + configName);
      }
      this.retryMaxAttempts = configValue;
    }
  }

  /**
   * Set the maximum retry wait time in milliseconds
   */
  public void setRetryMaxWaitTime(long maxWaitTime, String configName) {
    if (maxWaitTime > 0) {
      this.retryMaxWaitTime = maxWaitTime;
    } else {
      final Long configValue = applicationContext.getEnvironment().getProperty(configName, Long.class);
      if (Objects.isNull(configValue)) {
        throw new IllegalArgumentException("No config named " + configName);
      }
      this.retryMaxWaitTime = configValue;
    }
  }

  /**
   * Set the file name function
   */
  public void setFileNameFunction(Function<Message<?>, String> fileNameFunction) {
    this.fileNameFunction = fileNameFunction;
  }

  /**
   * Set the action
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * Set the completed action
   */
  public void setActionCompleted(String actionCompleted) {
    this.actionCompleted = actionCompleted;
  }

  /**
   * Subclasses may implement this for initialization logic.
   */
  @Override
  protected void onInit() {
    super.onInit();

    Assert.hasLength(this.ruleName, "Invalid ruleName");
    Assert.hasLength(this.action, "Invalid action");
    Assert.hasLength(this.actionCompleted, "Invalid actionCompleted");
    Assert.isTrue(this.retryMaxAttempts > 0, "Invalid retryMaxAttempts");
    Assert.isTrue(this.retryMaxWaitTime > 0, "Invalid retryMaxWaitTime");
    Assert.notNull(this.fileNameFunction, "Invalid fileNameFunction");

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(this.retryMaxAttempts);
    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(this.retryMaxWaitTime);
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.registerListener(this);
    this.setRetryTemplate(retryTemplate);
  }

  /**
   * Subclasses implement this method to apply behavior to the {@link MessageHandler}.
   * <p>
   * callback.execute() invokes the handler method and returns its result, or null.
   *
   * @param callback Subclasses invoke the execute() method on this interface to invoke the handler method.
   * @param target   The target handler.
   * @param message  The message that will be sent to the handler.
   * @return the result after invoking the {@link MessageHandler}.
   */
  @Override
  protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
    log.info("[{}] The maximum number of retry attempts including the initial attempt is {}, wait "
        + "time in milliseconds is {}", ruleName, retryMaxAttempts, retryMaxWaitTime);
    final String fileName = this.fileNameFunction.apply(message);
    try {
      Object result = super.doInvoke(callback, target, message);
      log.info("[{}] File {} has been {}", ruleName, fileName, actionCompleted);
      return result;
    } catch (Exception e) {
      log.error(String.format("[%s] File %s failed to %s after %s retry attempts",
          ruleName, fileName, action, retryMaxAttempts), e);
      return MessageBuilder.withPayload(message.getPayload())
          .copyHeaders(message.getHeaders())
          .setHeaderIfAbsent(SftpConstants.PROCESS_STATUS, SftpConstants.FAILED)
          .build();
    }
  }

  /**
   * Called after a successful attempt; allow the listener to throw a new exception to cause a retry (according to the retry
   * policy), based on the result returned by the {@link RetryCallback#doWithRetry(RetryContext)}
   *
   * @param <T>      the return type.
   * @param context  the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param result   the result returned by the callback method.
   * @since 2.0
   */
  @Override
  public <T, E extends Throwable> void onSuccess(RetryContext context,
      RetryCallback<T, E> callback, T result) {
    final Object message = context.getAttribute(SftpConstants.MESSAGE);
    final int retryCount = context.getRetryCount();
    if (Objects.nonNull(message) && retryCount > 0) {
      Message<?> requestMessage = (Message<?>) message;
      final String fileName = this.fileNameFunction.apply(requestMessage);
      log.info("[{}] File {} has been {} for the {} time",
          ruleName, fileName, actionCompleted, retryCount);
    }
  }

  /**
   * Called after every unsuccessful attempt at a retry.
   *
   * @param context   the current {@link RetryContext}.
   * @param callback  the current {@link RetryCallback}.
   * @param throwable the last exception that was thrown by the callback.
   * @param <T>       the return value
   * @param <E>       the exception to throw
   */
  @Override
  public <T, E extends Throwable> void onError(RetryContext context,
      RetryCallback<T, E> callback, Throwable throwable) {
    final Object message = context.getAttribute(SftpConstants.MESSAGE);
    final int retryCount = context.getRetryCount();
    if (Objects.nonNull(message) && retryCount > 0) {
      Message<?> requestMessage = (Message<?>) message;
      final String fileName = this.fileNameFunction.apply(requestMessage);
      log.error(String.format("[%s] File %s failed to %s for the %s time",
          ruleName, fileName, action, retryCount), throwable);
    }
  }

}
