package cn.maiaimei.samples.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
public class MessageChannelInterceptorTest {

  @Test
  public void testChannelInterceptor1() {
    QueueChannel inputChannel = new QueueChannel();
    inputChannel.setBeanName("inputChannel");
    inputChannel.setInterceptors(getInterceptors());
    inputChannel.send(MessageBuilder.withPayload("test").build());
    final Message<?> receiveMessage = inputChannel.receive();
    assertNotNull(receiveMessage);
    assertEquals("test", receiveMessage.getPayload());
  }

  @Test
  public void testChannelInterceptor2() {
    DirectChannel inputChannel = new DirectChannel();
    inputChannel.setBeanName("inputChannel");
    inputChannel.setInterceptors(getInterceptors());
    inputChannel.subscribe(message -> {
      log.info("Received message {}", message.getPayload());
      assertEquals("test", message.getPayload());
    });
    inputChannel.send(MessageBuilder.withPayload("test").build());
  }

  private List<ChannelInterceptor> getInterceptors() {
    List<ChannelInterceptor> interceptors = new ArrayList<>();
    ChannelInterceptor interceptor = new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info("preSend Message");
        return ChannelInterceptor.super.preSend(message, channel);
      }

      @Override
      public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        log.info("postSend Message");
        ChannelInterceptor.super.postSend(message, channel, sent);
      }

      @Override
      public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent,
          Exception ex) {
        log.info("afterSendCompletion Message");
        ChannelInterceptor.super.afterSendCompletion(message, channel, sent, ex);
      }

      @Override
      public boolean preReceive(MessageChannel channel) {
        log.info("preReceive Message");
        return ChannelInterceptor.super.preReceive(channel);
      }

      @Override
      public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        log.info("postReceive Message");
        return ChannelInterceptor.super.postReceive(message, channel);
      }

      @Override
      public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        log.info("afterReceiveCompletion Message");
        ChannelInterceptor.super.afterReceiveCompletion(message, channel, ex);
      }
    };
    interceptors.add(interceptor);
    return interceptors;
  }
}
