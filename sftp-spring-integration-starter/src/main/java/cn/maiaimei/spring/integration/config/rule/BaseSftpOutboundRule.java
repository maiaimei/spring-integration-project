package cn.maiaimei.spring.integration.config.rule;

import lombok.Data;

@Data
public class BaseSftpOutboundRule {

  private String schema;
  private String name;
  private String cron = "* * * * * ?";
  private long maxMessagesPerPoll = 100;
  private String pattern;
  private String local;
  private String remote;
  private String sent;
  private boolean acceptOnce = true;
}
