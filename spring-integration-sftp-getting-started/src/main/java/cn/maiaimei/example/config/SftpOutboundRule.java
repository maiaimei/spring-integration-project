package cn.maiaimei.example.config;

import lombok.Data;

@Data
public class SftpOutboundRule {

  private String cron = "* * * * * ?";
  private String local;
  private String pattern;
  private String destination;
  private String sent;
}
