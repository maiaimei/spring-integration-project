package cn.maiaimei.spring.integration.config;

import cn.maiaimei.spring.integration.sftp.config.SftpConfiguration;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import cn.maiaimei.spring.integration.sftp.factory.SftpInboundFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.file.remote.session.CachingSessionFactory;

@Import(SftpConfiguration.class)
@Configuration
public class SftpConfig {

  @Bean
  @ConfigurationProperties(prefix = "sftp.inbound.rules")
  public List<BaseSftpInboundRule> sftpInboundRules() {
    return new ArrayList<>();
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
