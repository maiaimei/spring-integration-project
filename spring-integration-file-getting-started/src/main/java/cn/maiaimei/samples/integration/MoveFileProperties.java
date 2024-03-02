package cn.maiaimei.samples.integration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "move-file")
public class MoveFileProperties {
  
  private String cron;
  private String source;
  private String pattern;
  private String destination;
}
