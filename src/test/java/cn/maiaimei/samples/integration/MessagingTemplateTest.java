package cn.maiaimei.samples.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class MessagingTemplateTest {

  @Test
  public void testSimpleSendAndReceive() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final QueueChannel channel = new QueueChannel();
    final MessagingTemplate template = new MessagingTemplate();
    ExecutorService exec = Executors.newSingleThreadExecutor();
    exec.execute(() -> {
      Message<?> message = template.receive(channel);
      if (message != null) {
        latch.countDown();
        assertThat(message.getPayload()).isEqualTo("testing");
      }
    });
    template.send(channel, new GenericMessage<>("testing"));
    assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
    exec.shutdownNow();
  }

}
