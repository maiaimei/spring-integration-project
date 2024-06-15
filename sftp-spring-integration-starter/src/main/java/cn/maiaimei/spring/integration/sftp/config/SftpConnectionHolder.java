package cn.maiaimei.spring.integration.sftp.config;

import java.util.Map;
import lombok.Data;

/**
 * the SFTP connections config holder
 */
@Data
public class SftpConnectionHolder {

  private Map<String, SftpConnection> connections;
}
