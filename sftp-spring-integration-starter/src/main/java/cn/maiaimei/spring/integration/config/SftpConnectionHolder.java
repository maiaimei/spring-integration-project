package cn.maiaimei.spring.integration.config;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * the SFTP connections config holder
 */
@Data
@ConfigurationProperties(prefix = "sftp")
public class SftpConnectionHolder {

  private Map<String, SftpConnection> connections;
}
