package cn.maiaimei.spring.integration.sftp.factory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.maiaimei.commons.lang.utils.IdGenerator;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.SftpTestSupport;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.sftp.config.SftpConnection;
import cn.maiaimei.spring.integration.sftp.config.SftpConnectionHolder;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ContextConfiguration(
    classes = {SftpInboundFactoryTest.ContextConfig.class}
)
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class SftpInboundFactoryTest extends SftpTestSupport {

  @Autowired
  private IntegrationFlowContext flowContext;

  @Autowired
  private SftpInboundFactory sftpInboundFactory;

  private File remoteSourceFile;
  private File localFile;
  private IntegrationFlowRegistration registration;

  @Override
  public void doSetupEnv() {
    // Create folder for test
    localFile = createLocalFolder("input");
    remoteSourceFile = createRemoteFolder("source");
    File remoteTempFile = createRemoteFolder("temp");
    File remoteArchiveFile = createRemoteFolder("archive");

    // Create inbound rule
    BaseSftpInboundRule rule = new BaseSftpInboundRule();
    rule.setId(IdGenerator.nextIdString());
    rule.setName("test-download");
    rule.setSchema(SFTP_SERVER_NAME);
    rule.setRemoteSource(remoteSourceFile.getAbsolutePath());
    rule.setRemoteTemp(remoteTempFile.getAbsolutePath());
    rule.setRemoteArchive(remoteArchiveFile.getAbsolutePath());
    rule.setLocal(localFile.getAbsolutePath());
    rule.setPattern("*.txt");
//    rule.setRenameExpression(
//        "test-in_${currentTimestamp->yyyyMMddHHmmssSSS}${serialNumber->%05d}.txt");
    rule.setRetryMaxAttempts(4);
    rule.setRetryMaxWaitTime(1000);

    // Register integration flow
    final IntegrationFlow flow = sftpInboundFactory.createSimpleSftpInboundFlow(rule);
    registration = flowContext.registration(flow).register();
  }

  @Override
  protected void doClearEnv() {
    // Destroy integration flow
    registration.destroy();
  }

  @Test
  public void testDownloadSingleFile()
      throws ExecutionException, InterruptedException, TimeoutException, IOException {
    // Prepare phase
    Path tempFile = Files.createTempFile(remoteSourceFile.toPath(), "TEST_DOWNLOAD_",
        ".txt");

    // Run async task to wait for expected files to be downloaded 
    // to a file system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      Path expectedFile = localFile.toPath().resolve(tempFile.getFileName());
      while (!Files.exists(expectedFile)) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(10, TimeUnit.SECONDS));
    assertTrue(Files.notExists(tempFile));
  }

  @Test
  public void testDownloadMultipleFiles()
      throws ExecutionException, InterruptedException, TimeoutException, IOException {
    // Prepare phase
    for (int i = 0; i < 10; i++) {
      Files.createTempFile(remoteSourceFile.toPath(), "TEST_DOWNLOAD_", ".txt");
    }

    // Run async task to wait for expected files to be downloaded 
    // to a file system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      while (localFile.listFiles().length < 10) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(10, TimeUnit.SECONDS));
  }

  @Import({
      TestApplication.class,
      SftpConfig.class
  })
  @TestConfiguration
  public static class ContextConfig {

    @Bean
    @Primary
    public SftpConnectionHolder sftpConnectionHolder() {
      final SftpConnection connection = new SftpConnection();
      connection.setHost(SFTP_SERVER_HOST);
      connection.setPort(SFTP_SERVER_PORT);
      connection.setUser(SFTP_SERVER_USER);
      connection.setPassword(SFTP_SERVER_PASSWORD);

      final SftpConnectionHolder holder = new SftpConnectionHolder();
      holder.setConnections(Maps.newHashMap());
      holder.getConnections().put(SFTP_SERVER_NAME, connection);
      return holder;
    }

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
