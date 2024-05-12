package sftp;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import cn.maiaimei.commons.lang.utils.StringUtils;
import cn.maiaimei.example.config.SftpConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.io.resource.AbstractIoResource;
import org.apache.sshd.common.util.io.resource.IoResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
public class SftpClientApplication {

  public static void main(String[] args) {
    SftpConnection conn = new SftpConnection();
    conn.setHost("127.0.0.1");
    conn.setPort(9090);
    conn.setUser("user");
    conn.setPrivateKey(new ClassPathResource("META-INF/keys/sftp_rsa"));
    conn.setPrivateKeyPassphrase("password");
    conn.setWaitTimeout(10000);

    uploadFile(conn);
  }

  private static void uploadFile(SftpConnection conn) {
    String destination = String.format("/path/to/destination/%s.txt", UUID.randomUUID());
    InputStream inputStream = new ByteArrayInputStream("foo".getBytes());
    execute(conn, session -> {
      log.info("Upload file start");
      try (SftpFileSystem fs = SftpClientFactory.instance().createSftpFileSystem(session)) {
        Path remoteRoot = fs.getDefaultDir().resolve(destination);
        if (!Files.exists(remoteRoot)) {
          Files.createDirectories(remoteRoot);
        }
        Path remoteFile = remoteRoot.resolve(destination);
        Files.copy(inputStream, remoteFile, REPLACE_EXISTING);
        log.info("Upload file success");
      } catch (Exception e) {
        log.error("Upload file failed", e);
      }
    });
  }

  private static void execute(SftpConnection conn, Consumer<ClientSession> consumer) {
    try (SshClient client = SshClient.setUpDefaultClient()) {
      log.info("SshClient is connecting");
      client.start();
      ClientSession session = client.connect(conn.getUser(), conn.getHost(), conn.getPort())
          .verify(conn.getWaitTimeout())
          .getSession();
      // 密码登录
      if (StringUtils.hasText(conn.getPassword())) {
        session.addPasswordIdentity(conn.getPassword());
      }
      // 公钥方式
      if (Objects.nonNull(conn.getPrivateKey())) {
        IoResource<Resource> privateKeyResource =
            new AbstractIoResource<>(Resource.class, conn.getPrivateKey()) {
              @Override
              public InputStream openInputStream() throws IOException {
                return getResourceValue().getInputStream();
              }
            };
        Collection<KeyPair> keys =
            SecurityUtils.getKeyPairResourceParser()
                .loadKeyPairs(null, privateKeyResource,
                    FilePasswordProvider.of(conn.getPrivateKeyPassphrase()));
        session.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keys));
      }
      session.auth().verify(conn.getWaitTimeout());
      log.info("SshClient connect success");
      consumer.accept(session);
    } catch (IOException | GeneralSecurityException e) {
      log.error("SshClient connect failed", e);
    }
  }

}
