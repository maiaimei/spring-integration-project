package cn.maiaimei.spring.integration.sftp.config;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class SftpConnection {

  private String host;
  private int port;
  private String user;
  private String password;
  private Resource privateKey;
  private String privateKeyPassphrase;
  private String proxyHost;
  private String proxyPort;
  private int poolSize;
  private long waitTimeout;
}
