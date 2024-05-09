package sftp;

import cn.maiaimei.samples.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Slf4j
public class SftpServerApplication {

  private static final SshServer server = SshServer.setUpDefaultServer();

  private static DefaultSftpSessionFactory sessionFactory;

  private static volatile int port;

  private static volatile String homeDirectory;

  private static volatile boolean running;

  /**
   * -Dport=9090 -DhomeDirectory=sftp-server-01 -Djava.io.tmpdir="C:\Users\lenovo\Desktop\tmp"
   */
  public static void main(String[] args) throws Exception {
    configureServer();
    startServer();
    final Scanner scanner = new Scanner(System.in);
    final String input = scanner.nextLine();
    if ("exit".equalsIgnoreCase(input)) {
      stopServer();
      System.exit(0);
    }
  }

  public static void startServer() {
    try {
      log.info("SftpServer is starting");
      server.start();
      sessionFactory.setPort(port);
      running = true;
      log.info("SftpServer started on port {}", port);
    } catch (IOException e) {
      log.error("SftpServer started error", e);
      throw new IllegalStateException(e);
    }
  }

  public static void stopServer() {
    if (running) {
      try {
        log.info("SftpServer is stoping");
        server.stop(true);
        log.info("SftpServer stop completed");
      } catch (Exception e) {
        log.error("SftpServer stop error", e);
        throw new IllegalStateException(e);
      } finally {
        running = false;
      }
    }
  }

  public static void configureServer() throws Exception {
    //printProperties();
    initProperties();
    initSessionFactory();
    String tmpdir = System.getProperty("java.io.tmpdir");
    final String pathname = tmpdir + File.separator + homeDirectory + File.separator;
    log.info("SftpServer home directory is {}", pathname);
    final File file = new File(pathname);
    if (!file.exists() && !file.mkdirs()) {
      throw new RuntimeException("SftpServer home directory create failed");
    }
    server.setPublickeyAuthenticator(getPublickeyAuthenticator());
    server.setPort(port);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
    server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
    server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(pathname)));
  }

  private static PublickeyAuthenticator getPublickeyAuthenticator() throws Exception {
    Path path = new ClassPathResource("META-INF/keys/sftp_known_hosts").getFile().toPath();
    return new AuthorizedKeysAuthenticator(path);
  }

  private static void initSessionFactory() {
    sessionFactory = new DefaultSftpSessionFactory();
    sessionFactory.setHost("127.0.0.1");
    sessionFactory.setUser("user");
    sessionFactory.setPrivateKey(new ClassPathResource("META-INF/keys/sftp_rsa"));
    sessionFactory.setPrivateKeyPassphrase("password");
    sessionFactory.setAllowUnknownKeys(Boolean.TRUE);
  }

  private static void initProperties() {
    final String portProperty = System.getProperty("port");
    if (StringUtils.hasText(portProperty)) {
      port = Integer.parseInt(portProperty);
    } else {
      port = getRandomPort();
    }
    final String homeDirectoryProperty = System.getProperty("homeDirectory");
    if (StringUtils.hasText(homeDirectoryProperty)) {
      homeDirectory = homeDirectoryProperty;
    } else {
      homeDirectory = String.format("sftp-server-%s", System.currentTimeMillis());
    }
    final String tmpdirProperty = System.getProperty("java.io.tmpdir");
    if (StringUtils.hasText(tmpdirProperty)) {
      System.setProperty("java.io.tmpdir", tmpdirProperty);
    }
  }

  private static void printProperties() {
    final Properties properties = System.getProperties();
    log.info("properties size: {}", properties.size());
    log.info("==================================================");
    properties.forEach((key, value) -> log.info("{} -> {}", key, value));
    log.info("==================================================");
  }

  private static int getRandomPort() {
    int randomPort = 0;
    // ServerSocket构造函数中的参数0指示操作系统自动分配一个随机的可用端口号。
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      randomPort = serverSocket.getLocalPort();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return randomPort;
  }
}
