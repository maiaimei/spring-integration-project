package cn.maiaimei.spring.integration.config.rule;

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
   * the files in this folder have not been sent to the remote host
   */
  private String send;
  /**
   * whether passes files only one time
   */
  private boolean acceptOnce = false;
}
