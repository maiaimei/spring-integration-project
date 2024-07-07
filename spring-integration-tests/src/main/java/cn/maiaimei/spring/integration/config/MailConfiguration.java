package cn.maiaimei.spring.integration.config;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfiguration {

  private Map<String, MailConnection> connections;
}
