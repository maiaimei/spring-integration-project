package cn.maiaimei.example.factory;

import cn.maiaimei.example.config.BaseSftpOutboundRule;
import cn.maiaimei.example.config.SftpConnection;
import cn.maiaimei.example.config.SftpConnectionHolder;
import cn.maiaimei.example.config.SimpleSftpOutboundRule;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;

/**
 * Spring Integration的Sftp.outboundGateway不支持直接嵌入SFTP服务器。
 * <p>
 * 通常，Sftp.outboundGateway用于连接到一个实际的SFTP服务器，如一个运行在远程服务器上的OpenSSH或其他SFTP服务器。
 * <p>
 * 如果你想要创建一个嵌入的SFTP服务器，你可能需要考虑使用一个第三方库，如Apache Mina SSHD。
 */
@Slf4j
@Component
@EnableConfigurationProperties(SftpConnectionHolder.class)
public class SftpOutboundFactory implements InitializingBean {

  private static final String PAYLOAD = "payload";

  private SftpConnectionHolder sftpConnectionHolder;

  private Map<String, CachingSessionFactory<SftpClient.DirEntry>> templateMap;

  @Override
  public void afterPropertiesSet() throws Exception {
//    templateMap = new HashMap<>();
//    sftpConnectionHolder.getConnections().forEach((schema, connection) -> {
//      templateMap.put(schema, cachingSessionFactory(connection));
//    });
  }

  public IntegrationFlow createSimpleSftpOutboundFlow(SimpleSftpOutboundRule rule) {
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(100)))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} detected in {}",
                rule.getName(), message.getHeaders().get(FileHeaders.FILENAME), rule.getLocal())
        ))
        .handle(Sftp.outboundGateway(template(rule), Command.PUT, PAYLOAD))
        .wireTap(flow -> flow.handle(
            message -> log.info("[{}] File {} has been uploaded to {}",
                rule.getName(), message.getHeaders().get(FileHeaders.FILENAME), rule.getRemote())
        ))
        .get();
  }

  private FileReadingMessageSource fileReadingMessageSource(BaseSftpOutboundRule rule) {
    CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new SimplePatternFileListFilter(rule.getPattern()));

    FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setDirectory(new File(rule.getLocal()));
    messageSource.setFilter(filter);
    return messageSource;
  }

  private RemoteFileTemplate<DirEntry> template(BaseSftpOutboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(
        templateMap.get(rule.getSchema()));
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getRemote()));
    return template;
  }

  private CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory(SftpConnection conn) {
    CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory =
        new CachingSessionFactory<>(defaultSftpSessionFactory(conn));
    cachingSessionFactory.setPoolSize(conn.getPoolSize());
    cachingSessionFactory.setSessionWaitTimeout(conn.getWaitTimeout());
    return cachingSessionFactory;
  }

  private DefaultSftpSessionFactory defaultSftpSessionFactory(SftpConnection conn) {
    DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
    sessionFactory.setHost(conn.getHost());
    sessionFactory.setPort(conn.getPort());
    sessionFactory.setUser(conn.getUser());
    sessionFactory.setPrivateKey(conn.getPrivateKey());
    sessionFactory.setPrivateKeyPassphrase(conn.getPrivateKeyPassphrase());
    sessionFactory.setAllowUnknownKeys(Boolean.TRUE);
    return sessionFactory;
  }

}
