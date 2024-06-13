package cn.maiaimei.spring.integration;

import cn.maiaimei.commons.lang.utils.FileUtils;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class FileGenerationTest {

  @Test
  public void test() {
    String pathname = "C:\\Users\\lenovo\\Desktop\\tmp\\input\\%s.txt";
    for (int i = 1; i <= 20; i++) {
      final String uuid = UUID.randomUUID().toString();
      FileUtils.writeStringToFile(String.format(pathname, uuid), uuid, StandardCharsets.UTF_8);
    }
  }
}
