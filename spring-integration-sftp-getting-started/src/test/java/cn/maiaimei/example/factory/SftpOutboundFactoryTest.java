package cn.maiaimei.example.factory;

import cn.maiaimei.example.TestConfig;
import cn.maiaimei.example.config.SftpOutboundRule;
import cn.maiaimei.samples.constants.StringConstants;
import cn.maiaimei.samples.utils.IOUtils;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.samples.sftp.SftpConstants;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SftpOutboundFactoryTest.ContextConfig.class})
public class SftpOutboundFactoryTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private IntegrationFlowContext flowContext;

  @Autowired
  private SftpOutboundFactory sftpOutboundFactory;

  @BeforeEach
  public void setUp() {
    // java -Djava.io.tmpdir=/path/to/tmpdir YourApp
    System.setProperty("java.io.tmpdir", SftpConstants.TMP_DIR);
  }

  @Test
  public void testOutbound() throws InterruptedException {
    String tmpdir = System.getProperty("java.io.tmpdir");

    SftpOutboundRule rule = new SftpOutboundRule();
    rule.setPattern("*.txt");
    rule.setLocal(tmpdir + "local");
    rule.setSent(tmpdir + "sent");
    rule.setDestination("/path/to/destination");

    final String uuid = UUID.randomUUID().toString();
    IOUtils.writeStringToFile(String.format("%s%s%s%s",
        rule.getLocal(), File.separator, uuid, StringConstants.FILE_SUFFIX_TXT), uuid);
    
    @SuppressWarnings("unchecked")
    SessionFactory<DirEntry> sessionFactory = applicationContext.getBean("sftpSessionFactory",
        CachingSessionFactory.class);
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(sessionFactory);
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getDestination()));
    sftpOutboundFactory.setTemplate(template);

    final IntegrationFlow flow = sftpOutboundFactory.create(rule);
    final String flowId = flowContext.registration(flow).register().getId();

    TimeUnit.SECONDS.sleep(3);

    flowContext.remove(flowId);
  }

  @Import(TestConfig.class)
  @ImportResource(locations = "classpath:META-INF/spring/integration/SftpSampleCommon.xml")
  public static class ContextConfig {

    @Bean
    public SftpOutboundFactory sftpOutboundFactory() {
      return new SftpOutboundFactory();
    }
  }
}
