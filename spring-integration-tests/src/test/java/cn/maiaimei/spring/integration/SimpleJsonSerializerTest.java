package cn.maiaimei.spring.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.json.SimpleJsonSerializer;

@Slf4j
public class SimpleJsonSerializerTest {

  @Test
  public void deserialize() throws JsonProcessingException {
    final String json = SimpleJsonSerializer.toJson(new TestFileInfo(), "fileInfo");
    log.info("{}", json);
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE);
    final TestFileInfo testFileInfo = objectMapper.readValue(json, TestFileInfo.class);
    assertEquals("foo.txt", testFileInfo.getFilename());
    assertEquals(213, testFileInfo.getSize());
  }

  public static class TestFileInfo extends AbstractFileInfo<String> {

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isLink() {
      return false;
    }

    @Override
    public long getSize() {
      return 213;
    }

    @Override
    public long getModified() {
      return 1718802895945L;
    }

    @Override
    public String getFilename() {
      return "foo.txt";
    }

    @Override
    public String getPermissions() {
      return "-rw-r--r--";
    }

    @Override
    public String getFileInfo() {
      return null;
    }
  }
}
