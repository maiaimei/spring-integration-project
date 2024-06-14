package cn.maiaimei.spring.integration.sftp.factory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.maiaimei.spring.integration.BaseTest;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.utils.ServerSocketUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ContextConfiguration(
    classes = {SftpInboundFactoryTest.ContextConfig.class}
)
@ExtendWith(SpringExtension.class)
public class SftpInboundFactoryTest extends BaseTest {

  private static final SshServer server = SshServer.setUpDefaultServer();
  private static volatile boolean isRunning;
  private static volatile int port;
  private static final String remote =
      TMP_DIR + File.separator + "sftp-server/jd";

  private static final String localDirectoryDownload = TMP_DIR + File.separator + "local";

  @BeforeAll
  public static void startServer() {
    port = ServerSocketUtils.getRandomPort();
    server.setPort(port);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("host.ser").toPath()));
    server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
    server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(remote)));
    server.setPasswordAuthenticator(
        (username, password, session) -> username.equals("test") && password.equals("password"));
    try {
      log.info("SftpServer is starting");
      server.start();
      isRunning = Boolean.TRUE;
      log.info("SftpServer started on port {}", port);
    } catch (IOException e) {
      log.error("SftpServer started error", e);
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void stopServer() throws IOException {
    if (isRunning) {
      server.stop();
    }
  }

  @BeforeEach
  @AfterEach
  public void clean() throws IOException {
    Files.walk(Paths.get(localDirectoryDownload))
        .filter(Files::isRegularFile)
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @Test
  public void testDownload()
      throws IOException, InterruptedException, TimeoutException, ExecutionException {
    // Prepare phase
    Path tempFile = Files.createTempFile(Path.of(remote), "TEST_DOWNLOAD_", ".xxx");

    // Run async task to wait for expected files to be downloaded to a file
    // system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      Path expectedFile = Paths.get(localDirectoryDownload).resolve(tempFile.getFileName());
      while (!Files.exists(expectedFile)) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(10, TimeUnit.SECONDS));
    assertTrue(Files.notExists(tempFile));
  }

  @Import({
      TestApplication.class,
      SftpConfig.class
  })
  @TestConfiguration
  public static class ContextConfig {

    @Bean
    public SftpInboundFactory sftpInboundFactory(ApplicationContext applicationContext,
        Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap) {
      final SftpInboundFactory factory = new SftpInboundFactory();
      factory.setApplicationContext(applicationContext);
      factory.setSessionFactoryMap(sessionFactoryMap);
      return factory;
    }
  }
}
