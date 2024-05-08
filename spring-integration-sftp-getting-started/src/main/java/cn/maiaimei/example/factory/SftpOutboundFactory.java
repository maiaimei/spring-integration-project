package cn.maiaimei.example.factory;

import cn.maiaimei.example.config.SftpOutboundRule;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * Spring Integration的Sftp.outboundGateway不支持直接嵌入SFTP服务器。
 * <p>
 * 通常，Sftp.outboundGateway用于连接到一个实际的SFTP服务器，如一个运行在远程服务器上的OpenSSH或其他SFTP服务器。
 * <p>
 * 如果你想要创建一个嵌入的SFTP服务器，你可能需要考虑使用一个第三方库，如Apache Mina SSHD。
 */
@Slf4j
public class SftpOutboundFactory implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  public IntegrationFlow create(SftpOutboundRule rule) {
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(100)))
        .wireTap(flow -> flow.handle(
            message -> log.info("File {} detected in {}",
                message.getHeaders().get(FileHeaders.FILENAME), rule.getLocal())
        ))
        .handle(Sftp.outboundGateway(template(rule), Command.PUT, "payload"))
        .wireTap(flow -> flow.handle(
            message -> log.info("File {} has been uploaded to {}",
                message.getHeaders().get(FileHeaders.FILENAME), rule.getDestination())
        ))
        .get();
  }

  private FileReadingMessageSource fileReadingMessageSource(SftpOutboundRule rule) {
    CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new SimplePatternFileListFilter(rule.getPattern()));

    FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setDirectory(new File(rule.getLocal()));
    messageSource.setFilter(filter);
    return messageSource;
  }

  private RemoteFileTemplate<DirEntry> template(SftpOutboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(sessionFactory());
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getDestination()));
    return template;
  }

  private DefaultSftpSessionFactory sessionFactory() {
    DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
    sessionFactory.setHost("127.0.0.1");
    sessionFactory.setPort(58351);
    sessionFactory.setUser("user");
    sessionFactory.setPrivateKey(new ClassPathResource("META-INF/keys/sftp_rsa"));
    sessionFactory.setPrivateKeyPassphrase("password");
    sessionFactory.setAllowUnknownKeys(Boolean.TRUE);
    return sessionFactory;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
