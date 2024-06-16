package cn.maiaimei.spring.integration.sftp.config.rule;

import lombok.Data;
import org.springframework.integration.scheduling.PollerMetadata;

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
  private String cron = "* * * * * ?";
  /**
   * max messages per poll
   */
  private long maxMessagesPerPoll = PollerMetadata.MAX_MESSAGES_UNBOUNDED;
  /**
   * Set the maximum number of objects the source should fetch if it is necessary to fetch objects.
   * Setting the maxFetchSize to 0 disables remote fetching, a negative value indicates no limit.
   */
  private int maxFetchSize = Integer.MIN_VALUE;
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
}
