package cn.maiaimei.spring.integration.factory;

import cn.maiaimei.commons.lang.constants.FileConstants;
import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.BaseTest;
import cn.maiaimei.spring.integration.TestApplication;
import cn.maiaimei.spring.integration.config.SftpConfig;
import cn.maiaimei.spring.integration.config.rule.SimpleSftpOutboundRule;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("local")
@PropertySource("/application-local.yml")
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
  public void testSimpleSftpOutbound() throws InterruptedException {
    String tmpdir = System.getProperty("java.io.tmpdir");

    SimpleSftpOutboundRule rule = new SimpleSftpOutboundRule();
    rule.setSchema("jd");
    rule.setName("jd-tms");
    rule.setPattern("{spring:\\S+.txt}");
    rule.setLocal(FileUtils.getFilePath(tmpdir, "local"));
    rule.setSent(FileUtils.getFilePath(tmpdir, "sent"));
    rule.setRemote("/path/to/destination");

    final String uuid = UUID.randomUUID().toString();
    String srcPath = FileUtils.getFileName(FileConstants.TXT, uuid, tmpdir, "processing");
    String destPath = FileUtils.getFileName(FileConstants.TXT, uuid, rule.getLocal());
    FileUtils.writeStringToFile(srcPath, uuid, StandardCharsets.UTF_8);
    FileUtils.moveFile(srcPath, destPath);

    final IntegrationFlow flow = sftpOutboundFactory.createSimpleSftpOutboundFlow(rule);
    final String flowId = flowContext.registration(flow).register().getId();

    TimeUnit.SECONDS.sleep(1);

    flowContext.remove(flowId);
  }

  @Import({
      TestApplication.class,
      SftpConfig.class,
      SftpOutboundFactory.class
  })
  public static class ContextConfig {

  }
}
