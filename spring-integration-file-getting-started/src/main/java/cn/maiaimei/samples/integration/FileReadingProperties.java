package cn.maiaimei.samples.integration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file-reading")
public class FileReadingProperties {

  private String cron;
  private String source;
  private String pattern;
}
