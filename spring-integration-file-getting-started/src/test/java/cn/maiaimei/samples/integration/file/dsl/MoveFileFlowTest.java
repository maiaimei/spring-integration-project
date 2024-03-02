package cn.maiaimei.samples.integration.file.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.maiaimei.samples.constants.IntegrationConstants;
import cn.maiaimei.samples.integration.MoveFileProperties;
import cn.maiaimei.samples.integration.config.IntegrationConfig;
import cn.maiaimei.samples.integration.config.IntegrationContextConfig01;
import cn.maiaimei.samples.utils.IOUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig(classes = MoveFileFlowTest.ContextConfig.class)
//@DirtiesContext
//@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class MoveFileFlowTest {

  @Autowired
  private MoveFileProperties moveFileProperties;

  @AfterAll
  public static void teardownAll() throws IOException {
    IOUtil.deleteDirectory(IOUtil.getOrCreateDirectory("/tmp/move-file"));
  }

  @Test
  public void testMoveFileFlow() throws InterruptedException {
    String name = UUID.randomUUID().toString();
    String extension = IntegrationConstants.FILE_SUFFIX_TXT;
    IOUtil.writeStringToFile(
        IOUtil.getPath(moveFileProperties.getSource(), name + extension), name
    );

    TimeUnit.SECONDS.sleep(3);

    final List<File> files = IOUtil.listFiles(moveFileProperties.getDestination());
    assertThat(files).isNotNull();
    assertEquals(1, files.size());
    log.info("Detected {} files in destination directory", files.size());
    files.forEach(file -> log.info("Detected file {} in destination directory", file.getAbsolutePath
        ()));
  }

  @Import({IntegrationConfig.class, IntegrationContextConfig01.class})
  public static class ContextConfig {

    @Bean
    public MoveFileProperties moveFileProperties() {
      MoveFileProperties properties = new MoveFileProperties();
      properties.setCron("* * * * * ?");
      properties.setSource("/tmp/move-file/input");
      properties.setPattern("*.txt");
      properties.setDestination("/tmp/move-file/archive");
      return properties;
    }
  }

}
