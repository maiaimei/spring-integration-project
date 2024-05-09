package cn.maiaimei.example.config;

import lombok.Data;

@Data
public class BaseSftpOutboundRule {

  private String schema;
  private String name;
  private String cron = "* * * * * ?";
  private Long maxMessagesPerPoll;
  private String pattern;
  private String local;
  private String remote;
  private String sent;
}
