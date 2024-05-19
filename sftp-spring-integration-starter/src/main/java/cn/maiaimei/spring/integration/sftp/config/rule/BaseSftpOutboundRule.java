package cn.maiaimei.spring.integration.sftp.config.rule;

import lombok.Data;

/**
 * Base SFTP outbound rule
 */
@Data
public class BaseSftpOutboundRule {

  /**
   * the schema for SFTP connection
   */
  private String schema;
  /**
   * the rule name
   */
  private String name;
  /**
   * the cron expression
   */
  private String cron = "* * * * * ?";
  /**
   * max messages per poll
   */
  private long maxMessagesPerPoll = 100;
  /**
   * the files match this pattern will send to remote host
   */
  private String pattern;
  /**
   * the files in this folder will send to remote host
   */
  private String local;
  /**
   * the path to save on the remote host
   */
  private String remote;
  /**
   * the files in this folder have been sent to remote host
   */
  private String sent;
  /**
   * whether passes files only one time
   */
  private boolean acceptOnce = false;
  /**
   * the maximum number of retry attempts including the initial attempt
   * <p>
   * includes the initial attempt before the retries begin so, generally, will be {@code >= 1}.
   * <p>
   * for example setting this property to 3 means 3 attempts total (initial + 2 retries).
   */
  private int maxRetries;
  /**
   * maximum retry wait time in milliseconds. Cannot be &lt; 1. Default value is 1000ms.
   */
  private long maxRetryWaitTime;
}
