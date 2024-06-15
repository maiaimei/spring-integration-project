package cn.maiaimei.spring.integration.sftp.factory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.SftpTestSupport;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.sftp.config.SftpConnection;
import cn.maiaimei.spring.integration.sftp.config.SftpConnectionHolder;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  @Test
  public void testCreateSimpleSftpInboundFlow()
      throws ExecutionException, InterruptedException, TimeoutException, IOException {
    final BaseSftpInboundRule rule = new BaseSftpInboundRule();
    rule.setName("test-sftp-download");
    rule.setSchema("foo");
    rule.setRemoteSource("/path/to/source");
    rule.setRemoteTemp("/path/to/temp");
    rule.setRemoteArchive("/path/to/archive");
    rule.setLocal(targetLocalDirectory.getAbsolutePath());
    rule.setPattern("*.xxx");
    final IntegrationFlow flow = sftpInboundFactory.createSimpleSftpInboundFlow(rule);
    IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();

    // Prepare phase
    Path tempFile = Files.createTempFile(getRemoteTempFolder().toPath(), "TEST_DOWNLOAD_", ".xxx");

    // Run async task to wait for expected files to be downloaded to a file
    // system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      Path expectedFile = Paths.get(getTargetLocalDirectoryName()).resolve(tempFile.getFileName());
      while (!Files.exists(expectedFile)) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(10, TimeUnit.SECONDS));
    assertTrue(Files.notExists(tempFile));

    registration.destroy();
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
      connection.setHost("localhost");
      connection.setPort(port);
      connection.setUser("foo");
      connection.setPassword("foo");

      final SftpConnectionHolder holder = new SftpConnectionHolder();
      holder.setConnections(Maps.newHashMap());
      holder.getConnections().put("foo", connection);
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
