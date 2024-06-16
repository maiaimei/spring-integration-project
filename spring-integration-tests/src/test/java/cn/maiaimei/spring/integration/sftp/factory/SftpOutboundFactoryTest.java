package cn.maiaimei.spring.integration.sftp.factory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.maiaimei.commons.lang.constants.NumberConstants;
import cn.maiaimei.commons.lang.utils.IdGenerator;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.SftpTestSupport;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.sftp.config.SftpConnection;
import cn.maiaimei.spring.integration.sftp.config.SftpConnectionHolder;
import cn.maiaimei.spring.integration.sftp.config.rule.SimpleSftpOutboundRule;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ContextConfiguration(
    classes = {SftpOutboundFactoryTest.ContextConfig.class}
)
@ExtendWith(SpringExtension.class)
public class SftpOutboundFactoryTest extends SftpTestSupport {

  @Autowired
  private IntegrationFlowContext flowContext;

  @Autowired
  private SftpOutboundFactory sftpOutboundFactory;

  private File remoteDestinationFile;
  private File localArchiveErrorFile;
  private IntegrationFlowRegistration registration;
  private SimpleSftpOutboundRule rule;

  @Override
  public void doSetupEnv() {
    File localFile = createLocalFolder("output");
    localArchiveErrorFile = createLocalFolder("sent", "error");
    remoteDestinationFile = createRemoteFolder("destination");
    String archive = createLocalFolder("sent").getAbsolutePath();

    // Prepare phase
    try {
      Files.createTempFile(localFile.toPath(), "TEST_UPLOAD_", ".txt");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    rule = new SimpleSftpOutboundRule();
    rule.setId(IdGenerator.nextIdString());
    rule.setName("test-upload");
    rule.setSchema(SFTP_SERVER_NAME);
    rule.setPattern("{spring:^\\S+.txt$}");
    rule.setLocal(localFile.getAbsolutePath());
    rule.setArchive(archive);
    rule.setRemote(remoteDestinationFile.getAbsolutePath());
    rule.setRetryMaxAttempts(4);
    rule.setRetryMaxWaitTime(1000);
  }

  @Override
  protected void doClearEnv() {
    // Destroy integration flow
    registration.destroy();
  }

  @Test
  public void testSimpleSftpOutbound() throws ExecutionException, InterruptedException, TimeoutException {
    IntegrationFlow flow = sftpOutboundFactory.createSimpleSftpOutboundFlow(rule);
    registration = flowContext.registration(flow).register();

    // Run async task to wait for expected files to be downloaded 
    // to a file system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      while (remoteDestinationFile.listFiles().length < 1) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(10, TimeUnit.SECONDS));
  }

  @Test
  public void testAdvancedSftpOutbound() throws ExecutionException, InterruptedException, TimeoutException {
    rule.setSchema("unknown-sftp");
    rule.setRetryMaxAttempts(4);
    rule.setRetryMaxWaitTime(1000);
    IntegrationFlow flow = sftpOutboundFactory.createAdvancedSftpOutboundFlow(rule);
    registration = flowContext.registration(flow).register();

    // Run async task to wait for expected files to be downloaded 
    // to a file system from a remote SFTP server
    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      while (localArchiveErrorFile.listFiles().length < 1) {
        TimeUnit.MILLISECONDS.sleep(200);
      }
      return Boolean.TRUE;
    });

    // Validation phase
    assertTrue(future.get(20, TimeUnit.SECONDS));
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

      final SftpConnection unknownConnection = new SftpConnection();
      unknownConnection.setHost(SFTP_SERVER_HOST);
      unknownConnection.setPort(NumberConstants.ONE);
      unknownConnection.setUser(SFTP_SERVER_USER);
      unknownConnection.setPassword(SFTP_SERVER_PASSWORD);

      final SftpConnectionHolder holder = new SftpConnectionHolder();
      holder.setConnections(Maps.newHashMap());
      holder.getConnections().put(SFTP_SERVER_NAME, connection);
      holder.getConnections().put("unknown-sftp", unknownConnection);
      return holder;
    }

    @Bean
    public SftpOutboundFactory sftpOutboundFactory(ApplicationContext applicationContext,
        Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap) {
      final SftpOutboundFactory factory = new SftpOutboundFactory();
      factory.setApplicationContext(applicationContext);
      factory.setSessionFactoryMap(sessionFactoryMap);
      return factory;
    }
  }
}
