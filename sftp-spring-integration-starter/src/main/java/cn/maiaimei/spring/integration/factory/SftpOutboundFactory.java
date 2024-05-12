package cn.maiaimei.spring.integration.factory;

import cn.maiaimei.spring.integration.config.rule.BaseSftpOutboundRule;
import cn.maiaimei.spring.integration.config.rule.SimpleSftpOutboundRule;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
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
public class SftpOutboundFactory implements ApplicationContextAware {

  private static final String PAYLOAD = "payload";

  private ApplicationContext applicationContext;

  @Autowired
  private Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * 在Spring Integration中，Sftp.outboundGateway用于通过SFTP协议与远程服务器进行文件传输。
   * 由于Sftp.outboundGateway是一个请求-响应模式的消息传递组件，它期望获得一个响应。
   * 如果你不想发送响应，可以通过配置reply-channel属性指定一个不存在的通道，或者使用null-channel。
   */
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
        .handle(message -> MessageBuilder.fromMessage(message).setHeaderIfAbsent(
            MessageHeaders.REPLY_CHANNEL, "nullChannel"))
        .get();
  }

  private FileReadingMessageSource fileReadingMessageSource(BaseSftpOutboundRule rule) {
    CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new SimplePatternFileListFilter(rule.getPattern()));
    filter.addFilter(new AcceptOnceFileListFilter<>()); // 接受一次性特性
    //filter.addFilter(new FileSystemPersistentAcceptOnceFileListFilter()); // 持久化标记

    FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setDirectory(new File(rule.getLocal()));
    messageSource.setFilter(filter);
    return messageSource;
  }

  /**
   * RemoteFileTemplate 是Spring Integration中用于与远程文件系统交互的模板类。
   * <p>
   * 它提供了一种方便的方式来执行文件操作，如读取、写入和删除文件。
   */
  private RemoteFileTemplate<DirEntry> template(BaseSftpOutboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(
        sessionFactoryMap.get(rule.getSchema()));
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getRemote()));
    template.setUseTemporaryFileName(Boolean.TRUE);
    template.setBeanFactory(applicationContext);
    return template;
  }

}