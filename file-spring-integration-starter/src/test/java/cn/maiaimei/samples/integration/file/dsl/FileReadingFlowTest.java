package cn.maiaimei.samples.integration.file.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import cn.maiaimei.commons.lang.constants.FileConstants;
import cn.maiaimei.commons.lang.constants.StringConstants;
import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.samples.integration.FileReadingProperties;
import cn.maiaimei.samples.integration.config.IntegrationConfig;
import cn.maiaimei.samples.integration.config.IntegrationContextConfig02;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig(classes = FileReadingFlowTest.ContextConfig.class)
//@DirtiesContext
public class FileReadingFlowTest {

  @Autowired
  private FileReadingProperties fileReadingProperties;

  @Autowired
  private PollableChannel errorChannel;

  @Autowired
  @Qualifier("fileReadingResultChannel")
  private PollableChannel fileReadingResultChannel;

  @AfterAll
  public static void teardownAll() throws IOException {
    FileUtils.deleteDirectory(FileUtils.getOrCreateDirectory("/tmp/file-reading"));
  }

  @Test
  public void testFileReadingFlow() {
    // create files
    List<Integer> evens = new ArrayList<>(25);
    for (int i = 0; i < 10; i++) {
      boolean even = i % 2 == 0;
      String extension = String.format("%s%s", StringConstants.DOT,
          even ? FileConstants.TXT : FileConstants.CSV);
      if (even) {
        evens.add(i);
      }
      FileUtils.writeStringToFile(
          FileUtils.getPath(fileReadingProperties.getSource(), i + extension),
          String.valueOf(i),
          StandardCharsets.UTF_8
      );
    }

    // success scenario
    Message<?> message = fileReadingResultChannel.receive(60000);
    assertThat(message).isNotNull();
    Object payload = message.getPayload();
    assertThat(payload).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) payload;
    assertThat(result.size()).isEqualTo(5);
    result.forEach(s -> assertThat(evens.contains(Integer.parseInt(s))).isTrue());

    // error scenario
    FileUtils.getOrCreateFile(fileReadingProperties.getSource(), "a.txt");
    Message<?> receive = this.errorChannel.receive(60000);
    assertThat(receive).isNotNull();
    assertThat(receive).isInstanceOf(ErrorMessage.class);
  }

  @Import({IntegrationConfig.class, IntegrationContextConfig02.class})
  public static class ContextConfig {

    @Bean
    public FileReadingProperties moveFileProperties() {
      FileReadingProperties properties = new FileReadingProperties();
      properties.setCron("* * * * * ?");
      properties.setSource("/tmp/file-reading/input");
      properties.setPattern("*.txt");
      return properties;
    }
  }

}
