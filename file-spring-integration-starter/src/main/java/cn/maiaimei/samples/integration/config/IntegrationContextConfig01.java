package cn.maiaimei.samples.integration.config;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.samples.constants.IntegrationConstants;
import cn.maiaimei.samples.integration.MoveFileProperties;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

@Slf4j
@Configuration
public class IntegrationContextConfig01 {

  @Autowired
  private MoveFileProperties moveFileProperties;

  @Bean
  public IntegrationFlow moveFileFlow() {
    return IntegrationFlow
        .from(fileReadingMessageSource(),
            e -> e.poller(Pollers.cron(moveFileProperties.getCron())
                .errorChannel(IntegrationConstants.ERROR_CHANNEL)))
        .wireTap(
            flow -> flow.handle(message -> log.info("Detected file {}", message.getPayload()))
        )
        .handle(fileWritingMessageHandler())
        .get();
  }

  private FileReadingMessageSource fileReadingMessageSource() {
    final CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new IgnoreHiddenFileListFilter());
    filter.addFilter(new AcceptOnceFileListFilter<>());
    filter.addFilter(new SimplePatternFileListFilter(moveFileProperties.getPattern()));
    final FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setDirectory(
        FileUtils.getOrCreateDirectory(moveFileProperties.getSource())
    );
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setFilter(filter);
    messageSource.setUseWatchService(Boolean.TRUE);
    messageSource.setWatchEvents(
        FileReadingMessageSource.WatchEventType.CREATE,
        FileReadingMessageSource.WatchEventType.MODIFY
    );
    return messageSource;
  }

  private FileWritingMessageHandler fileWritingMessageHandler() {
    FileWritingMessageHandler handler = new FileWritingMessageHandler(
        FileUtils.getOrCreateDirectory(moveFileProperties.getDestination())
    );
    handler.setAutoCreateDirectory(Boolean.TRUE);
    handler.setDeleteSourceFiles(Boolean.TRUE);
    handler.setExpectReply(Boolean.FALSE);
    return handler;
  }

}
