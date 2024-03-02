package cn.maiaimei.samples.integration.config;

import cn.maiaimei.samples.constants.IntegrationConstants;
import cn.maiaimei.samples.integration.FileReadingProperties;
import cn.maiaimei.samples.utils.IOUtil;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.Files;

@Slf4j
@Configuration
public class IntegrationContextConfig02 {

  @Autowired
  private FileReadingProperties fileReadingProperties;

  @Bean
  public IntegrationFlow fileReadingFlow() {
    return IntegrationFlow
        .from(
            Files.inboundAdapter(IOUtil.getOrCreateDirectory(fileReadingProperties.getSource()))
                .patternFilter(fileReadingProperties.getPattern())
                .useWatchService(Boolean.TRUE)
                .watchEvents(
                    FileReadingMessageSource.WatchEventType.CREATE,
                    FileReadingMessageSource.WatchEventType.MODIFY
                ),
            e -> e.poller(Pollers.cron(fileReadingProperties.getCron())
                .errorChannel(IntegrationConstants.ERROR_CHANNEL)
            ))
        .wireTap(
            flow -> flow.handle(message -> log.info("Detected file {}", message.getPayload()))
        )
        .filter(File.class,
            p -> !p.getName().startsWith("a"),
            e -> e.throwExceptionOnRejection(Boolean.TRUE)
        )
        .wireTap(
            flow -> flow.handle(message -> log.info("Filtering file {}", message.getPayload()))
        )
        .transform(Files.toStringTransformer())
        .wireTap(
            flow -> flow.handle(message -> log.info("Show file {}", message.getPayload()))
        )
        .aggregate(a -> a.correlationExpression("1")
            .releaseStrategy(g -> g.size() == 5))
        .wireTap(
            flow -> flow.handle(
                message -> log.info("Show files {}", message.getPayload())
            )
        )
        .channel(MessageChannels.queue("fileReadingResultChannel"))
        .get();
  }

}
