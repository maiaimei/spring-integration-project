package cn.maiaimei.spring.integration.config;

import java.util.Map;
import lombok.Data;

@Data
public class MailConnection {

  private String host;
  private Integer port;
  private String username;
  private String password;
  private String protocol;
  private Map<String, String> properties;

  /**
   * If your username contains the '@' character,
   * <p>
   * use '%40' instead of '@' to avoid parsing errors from the underlying JavaMail API.
   *
   * @return
   */
  public String getEncodeUsername() {
    return username.replaceAll("@", "%40");
  }

}
