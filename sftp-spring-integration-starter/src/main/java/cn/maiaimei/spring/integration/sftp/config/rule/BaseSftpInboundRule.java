package cn.maiaimei.spring.integration.sftp.config.rule;

import lombok.Data;

/**
 * BaseSftpInboundRule
 */
@Data
public class BaseSftpInboundRule {

  /**
   * the rule id
   */
  private String id;
  /**
   * the rule name
   */
  private String name;
  /**
   * the schema for SFTP connection
   */
  private String schema;
  /**
   * the cron expression
   */
  private String cron;
  /**
   * max messages per poll
   */
  private long maxMessagesPerPoll;
  /**
   * the source path to download files on remote host
   */
  private String remoteSource;
  /**
   * the temp path of downloading files on remote host
   */
  private String remoteTemp;
  /**
   * the archive path of downloaded files on remote host
   */
  private String remoteArchive;
  /**
   * whether archive files by date
   */
  private boolean archiveByDate;
  /**
   * the path of downloaded files on local host
   */
  private String local;
  /**
   * the files match this pattern will send to remote host
   */
  private String pattern;
  /**
   * Specify a SpEL expression for files renaming during download.
   */
  private String renameExpression;
  /**
   * ready file suffix
   */
  private String readyFileSuffix;
  /**
   * the maximum number of retry attempts including the initial attempt
   * <p>
   * includes the initial attempt before the retries begin so, generally, will be {@code >= 1}.
   * <p>
   * for example setting this property to 3 means 3 attempts total (initial + 2 retries).
   */
  private int retryMaxAttempts;
  /**
   * maximum retry wait time in milliseconds. Cannot be &lt; 1. Default value is 1000ms.
   */
  private long retryMaxWaitTime;
}
