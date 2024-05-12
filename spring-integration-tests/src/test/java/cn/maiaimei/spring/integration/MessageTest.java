package cn.maiaimei.spring.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * Message创建之后不允许修改，无论是headers还是payload。
 * 这是因为同一个Message有可能分发给多个channel，为了保持数据一致性，就不允许中途修改数据了。如果有修改需求，只能重新创建一个Message。使用MessageBuilder
 * 可以很方便的修改headers还是payload。
 */
public class MessageTest {

  @Test
  public void testGenericMessage1() {
    Message<String> message = new GenericMessage<>("test");
    assertEquals("test", message.getPayload());
  }

  @Test
  public void testGenericMessage2() {
    Map<String, Object> map = new HashMap<>();
    map.put("content-type", "text");
    Message<String> message = new GenericMessage<>("test", map);
    assertEquals("text", message.getHeaders().get("content-type"));
    assertEquals("test", message.getPayload());
  }

  @Test
  public void testGenericMessage3() {
    Map<String, Object> map = new HashMap<>();
    map.put("content-type", "text");
    MessageHeaders headers = new MessageHeaders(map);
    Message<String> message = new GenericMessage<>("test", headers);
    assertEquals("text", message.getHeaders().get("content-type"));
    assertEquals("test", message.getPayload());
  }

  @Test
  public void testMessageBuilder() {
    final Message<String> message1 = MessageBuilder.withPayload("test")
        .setHeader("content-type", "text")
        .build();
    assertEquals("text", message1.getHeaders().get("content-type"));
    assertEquals("test", message1.getPayload());

    final Message<String> message2 = MessageBuilder.fromMessage(message1)
        .setHeader("content-type", "plaintext")
        .build();
    assertEquals("plaintext", message2.getHeaders().get("content-type"));
    assertEquals("test", message2.getPayload());
  }

}
