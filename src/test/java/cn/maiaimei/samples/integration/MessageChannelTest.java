package cn.maiaimei.samples.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

/**
 * {@link MessageChannel} {@link PollableChannel} {@link SubscribableChannel}
 */
@Slf4j
public class MessageChannelTest {

  @Test
  public void testQueueChannel() {
    QueueChannel inputChannel = new QueueChannel();
    inputChannel.send(MessageBuilder.withPayload("test").build());
    final Message<?> receiveMessage = inputChannel.receive();
    assertNotNull(receiveMessage);
    assertEquals("test", receiveMessage.getPayload());
  }

  @Test
  public void testDirectChannel() {
    DirectChannel inputChannel = new DirectChannel();
    inputChannel.setBeanName("inputChannel");
    inputChannel.subscribe(message -> {
      log.info("Received message {}", message.getPayload());
      assertEquals("test", message.getPayload());
    });
    inputChannel.send(MessageBuilder.withPayload("test").build());
  }

  @Test
  public void testPublishSubscribeChannel() {
    PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();
    inputChannel.setBeanName("inputChannel");

    MessageHandler subscriber1 = message -> {
      log.info("Subscriber 1 received message {}", message.getPayload());
      assertEquals("test", message.getPayload());
    };
    MessageHandler subscriber2 = message -> {
      log.info("Subscriber 2 received message {}", message.getPayload());
      assertEquals("test", message.getPayload());
    };
    MessageHandler subscriber3 = message -> {
      log.info("Subscriber 3 received message {}", message.getPayload());
      assertEquals("test", message.getPayload());
    };
    inputChannel.subscribe(subscriber1);
    inputChannel.subscribe(subscriber2);
    inputChannel.subscribe(subscriber3);
    inputChannel.send(MessageBuilder.withPayload("test").build());
    inputChannel.unsubscribe(subscriber3);
    inputChannel.send(MessageBuilder.withPayload("test").build());
  }

  @Test
  public void testSimpleSendAndReceive() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final QueueChannel channel = new QueueChannel();
    ExecutorService exec = Executors.newSingleThreadExecutor();
    exec.execute(() -> {
      Message<?> message = channel.receive();
      if (message != null) {
        latch.countDown();
        assertThat(message.getPayload()).isEqualTo("testing");
      }
    });
    channel.send(new GenericMessage<>("testing"));
    assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
    exec.shutdownNow();
  }

}
