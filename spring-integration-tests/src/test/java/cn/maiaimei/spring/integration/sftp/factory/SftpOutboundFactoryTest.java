package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.constants.FileConstants;
import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.BaseTest;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.sftp.config.SftpConfig;
import cn.maiaimei.spring.integration.sftp.config.rule.SimpleSftpOutboundRule;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.util.CollectionUtils;

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

  private String tmpdir;
  private SimpleSftpOutboundRule rule;
  private final int count = 5;

  @BeforeEach
  public void setUp() {
    // java -Djava.io.tmpdir=/path/to/tmpdir YourApp
    System.setProperty("java.io.tmpdir", TMP_DIR);

    tmpdir = System.getProperty("java.io.tmpdir");
    String processing = FileUtils.getFilePath(tmpdir, "processing");
    String local = FileUtils.getFilePath(tmpdir, "local");
    String archive = FileUtils.getFilePath(tmpdir, "sent");

    for (int i = 0; i < count; i++) {
      final String uuid = UUID.randomUUID().toString();
      String srcFileName = FileUtils.getFileName(FileConstants.TXT, uuid, processing);
      String destFileName = FileUtils.getFileName(FileConstants.TXT, uuid, local);
      FileUtils.writeStringToFile(srcFileName, uuid, StandardCharsets.UTF_8);
      FileUtils.moveFile(srcFileName, destFileName);
    }

    rule = new SimpleSftpOutboundRule();
    rule.setName("jd-tms");
    rule.setPattern("{spring:^\\S+.txt$}");
    rule.setLocal(local);
    rule.setArchive(archive);
    rule.setRemote("/path/to/destination");
  }

  @AfterEach
  public void tearDown() {
    sessionFactoryMap.forEach((schema, factory) -> factory.destroy());

    FileUtils.cleanDirectory(rule.getArchive());
    FileUtils.cleanDirectory(FileUtils.getFilePath(tmpdir, "sftp-server/jd/path/to/destination"));
  }

  @Test
  public void testSimpleSftpOutbound() {
    rule.setSchema("jd");
    IntegrationFlow flow = sftpOutboundFactory.createSimpleSftpOutboundFlow(rule);
    flowContext.registration(flow).register();
    Assertions.assertTrue(await());
  }

  @Test
  public void testAdvancedSftpOutbound() {
    rule.setSchema("unknown-host");
    IntegrationFlow flow = sftpOutboundFactory.createAdvancedSftpOutboundFlow(rule);
    flowContext.registration(flow).register();
    Assertions.assertTrue(await());
  }

  private boolean await() {
    String sentPath = FileUtils.getFilePath(tmpdir, "sent");
    final ArrayList<String> invalidSchema = Lists.newArrayList(
        "unknown-host", "unknown-port", "unknown-keys"
    );
    if (invalidSchema.contains(rule.getSchema())) {
      sentPath = FileUtils.getFilePath(tmpdir, "sent", "error");
    }
    while (true) {
      final List<File> files = FileUtils.listFiles(sentPath);
      if (!CollectionUtils.isEmpty(files) && files.size() == count) {
        break;
      }
    }
    return Boolean.TRUE;
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
