package cn.maiaimei.example.factory;

import cn.maiaimei.example.config.SftpOutboundRule;
import java.io.File;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.sftp.dsl.Sftp;

@Slf4j
public class SftpOutboundFactory {

  @Setter
  RemoteFileTemplate<DirEntry> template;

  public IntegrationFlow create(SftpOutboundRule rule) {
    return IntegrationFlow.from(fileReadingMessageSource(rule),
            e -> e.poller(p -> p.cron(rule.getCron()).maxMessagesPerPoll(100)))
        .wireTap(flow -> flow.handle(
            message -> log.info("File {} detected in {}",
                message.getHeaders().get(FileHeaders.FILENAME), rule.getLocal())
        ))
        .handle(Sftp.outboundGateway(template, Command.PUT, "payload"))
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
}
