package sftp;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class SftpConnection {

  private String host;
  private int port;
  private String user;
  private String password;
  private Resource privateKey;
  private String PrivateKeyPassphrase;
  private String proxyHost;
  private String proxyPort;
  private Integer maxSession;
  private long timeout;
}
