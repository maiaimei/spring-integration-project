package cn.maiaimei.samples.integration.file.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import cn.maiaimei.samples.integration.config.IntegrationConfig;
import cn.maiaimei.samples.utils.IOUtil;
import cn.maiaimei.samples.utils.StringUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = FileWritingFlowTest.ContextConfig.class)
public class FileWritingFlowTest {

  @Autowired
  @Qualifier("fileWritingInput")
  public MessageChannel fileWritingInput;

  @Autowired
  @Qualifier("fileWritingResultChannel")
  public PollableChannel fileWritingResultChannel;

  @AfterAll
  public static void teardownAll() throws IOException {
    IOUtil.deleteDirectory(IOUtil.getOrCreateDirectory("/tmp/file-writing"));
  }

  @Test
  public void testFileWritingFlow() throws InterruptedException {
    fileWritingInput.send(new GenericMessage<>("foo"));
    TimeUnit.SECONDS.sleep(3);
    final List<File> files = IOUtil.listFiles("/tmp/file-writing");
    assertThat(files).isNotNull();
    assertThat(files.size()).isEqualTo(1);
    final Message<?> receive = fileWritingResultChannel.receive(60000);
    assertThat(receive).isNotNull();
    assertThat(receive.getPayload()).isNotNull();
    assertThat(StringUtil.cleanPath(receive.getPayload().toString())).isEqualTo(
        "/tmp/file-writing/foo.txt");
  }

  @Import({IntegrationConfig.class})
  public static class ContextConfig {

    @Bean
    public IntegrationFlow fileWritingFlow() {
      return IntegrationFlow.from("fileWritingInput")
          .enrichHeaders(h -> h.header(FileHeaders.FILENAME, "foo.txt")
              .header("directory", IOUtil.getOrCreateDirectory("/tmp/file-writing")))
          .handle(Files.outboundGateway(m -> m.getHeaders().get("directory")))
          .channel(MessageChannels.queue("fileWritingResultChannel"))
          .get();
    }
  }
}
