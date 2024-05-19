package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.constants.FileConstants;
import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.BaseTest;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.sftp.config.rule.SimpleSftpOutboundRule;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ActiveProfiles(profiles = {"default", "local", "test"})
@PropertySource("/config/application-local.yml")
@TestPropertySource("/config/application-test.yml")
@ContextConfiguration(
    classes = {SftpOutboundFactoryTest.ContextConfig.class}, // 用来加载配置类
    initializers = ConfigDataApplicationContextInitializer.class // 用来加载配置文件
)
@ExtendWith(SpringExtension.class)
public class SftpOutboundFactoryTest extends BaseTest {

  @Autowired
  private IntegrationFlowContext flowContext;

  @Autowired
  private SftpOutboundFactory sftpOutboundFactory;

  @Autowired
  private Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap;

  @BeforeEach
  public void setUp() {
    // java -Djava.io.tmpdir=/path/to/tmpdir YourApp
    System.setProperty("java.io.tmpdir", TMP_DIR);
  }

  @AfterEach
  public void tearDown() {
    sessionFactoryMap.forEach((schema, factory) -> factory.destroy());
  }

  @Test
  public void testSimpleSftpOutbound() {
    String tmpdir = getTmpDir();

    // create rule
    SimpleSftpOutboundRule rule = new SimpleSftpOutboundRule();
    rule.setSchema("jd");
    rule.setName("jd-tms");
    rule.setPattern("{spring:\\S+.txt}");
    rule.setLocal(FileUtils.getFilePath(tmpdir, "local"));
    rule.setSent(FileUtils.getFilePath(tmpdir, "sent"));
    rule.setRemote("/path/to/destination");

    // create test file
    final String uuid = UUID.randomUUID().toString();
    String srcPath = FileUtils.getFileName(FileConstants.TXT, uuid, tmpdir, "processing");
    String destPath = FileUtils.getFileName(FileConstants.TXT, uuid, rule.getLocal());
    FileUtils.writeStringToFile(srcPath, uuid, StandardCharsets.UTF_8);
    FileUtils.moveFile(srcPath, destPath);

    // register integration flow
    final IntegrationFlow flow = sftpOutboundFactory.createSimpleSftpOutboundFlow(rule);
    flowContext.registration(flow).register();

    // block program until the file is processed
    final String remotePath = FileUtils.getFilePath(tmpdir, "sftp-server/jd/path/to/destination");
    final String remoteFile = FileUtils.getFileName(FileConstants.TXT, uuid, remotePath);
    while (true) {
      if (new File(remoteFile).exists()) {
        break;
      }
    }

    // delete the test file
    FileUtils.cleanDirectory(remotePath);
    FileUtils.cleanDirectory(rule.getSent());
  }

  @Test
  public void testAdvancedSftpOutbound() {
    String tmpdir = getTmpDir();

    // create rule
    SimpleSftpOutboundRule rule = new SimpleSftpOutboundRule();
    rule.setSchema("jd");
    //rule.setSchema("unknown-host");
    //rule.setSchema("unknown-port");
    //rule.setSchema("unknown-keys");
    rule.setName("jd-tms");
    rule.setPattern("{spring:\\S+.txt}");
    rule.setLocal(FileUtils.getFilePath(tmpdir, "local"));
    rule.setSent(FileUtils.getFilePath(tmpdir, "sent"));
    rule.setRemote("/path/to/destination");

    // create test file
    final String uuid = UUID.randomUUID().toString();
    String srcPath = FileUtils.getFileName(FileConstants.TXT, uuid, tmpdir, "processing");
    String destPath = FileUtils.getFileName(FileConstants.TXT, uuid, rule.getLocal());
    FileUtils.writeStringToFile(srcPath, uuid, StandardCharsets.UTF_8);
    FileUtils.moveFile(srcPath, destPath);

    // register integration flow
    final IntegrationFlow flow = sftpOutboundFactory.createAdvancedSftpOutboundFlow(rule);
    flowContext.registration(flow).register();

    // block program until the file is processed
    final String sentPath1 = FileUtils.getFilePath(tmpdir, "sent");
    final String sentPath2 = FileUtils.getFilePath(tmpdir, "sent", "error");
    final String sentPath3 = FileUtils.getFilePath(tmpdir, "sftp-server/jd/path/to/destination");
    final String fileName1 = FileUtils.getFileName(FileConstants.TXT, uuid, sentPath1);
    final String fileName2 = FileUtils.getFileName(FileConstants.TXT, uuid, sentPath2);
    final String fileName3 = FileUtils.getFileName(FileConstants.TXT, uuid, sentPath3);
    while (true) {
      if (new File(fileName1).exists() || new File(fileName2).exists() || new File(
          fileName3).exists()) {
        break;
      }
    }

    // delete the test file
    FileUtils.cleanDirectory(sentPath1);
    FileUtils.cleanDirectory(sentPath3);
    FileUtils.cleanDirectory(rule.getLocal());
  }

  private String getTmpDir() {
    return System.getProperty("java.io.tmpdir");
  }

  @Import({
      TestApplication.class,
      SftpConfig.class
  })
  @TestConfiguration
  public static class ContextConfig {

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
