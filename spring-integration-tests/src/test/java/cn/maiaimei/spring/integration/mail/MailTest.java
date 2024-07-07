package cn.maiaimei.spring.integration.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.maiaimei.spring.integration.FileTestSupport;
import cn.maiaimei.spring.integration.TestIntegrationConfig;
import cn.maiaimei.spring.integration.config.MailConfiguration;
import cn.maiaimei.spring.integration.config.MailConnection;
import cn.maiaimei.spring.integration.mail.MailTest.ContextConfig;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * https://docs.spring.io/spring-integration/reference/mail.html
 */
@Slf4j
@ActiveProfiles({"qq"})
@ContextConfiguration(
    classes = ContextConfig.class,
    initializers = ConfigDataApplicationContextInitializer.class
)
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class MailTest extends FileTestSupport {

  @Value("${mail.recipients.to:unknown}")
  private String to;

  @Value("${mail.recipients.cc:unknown}")
  private String cc;

  @Value("${mail.recipients.bcc:unknown}")
  private String bcc;

  @Autowired
  private MailConfiguration mailConfiguration;

  @Autowired
  private MailProperties mailProperties;

  @Autowired
  private IntegrationFlowContext flowContext;

  private IntegrationFlowRegistration registration;

  @Test
  public void testReceiveMailByIMAP() throws ExecutionException, InterruptedException, TimeoutException {
    final MailConnection mailConnection = mailConfiguration.getConnections().get("test-imap");
    registration = flowContext.registration(imapMailFlow(mailConnection)).register();

    Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
      TimeUnit.SECONDS.sleep(60);
      return Boolean.TRUE;
    });

    assertTrue(future.get(60, TimeUnit.SECONDS));
  }

  @Override
  protected void doTearDown() {
    registration.destroy();
  }

  private IntegrationFlow imapMailFlow(MailConnection mailConnection) {
    String url = String.format("imap://%s:%s@%s:%s/%s",
        mailConnection.getEncodeUsername(),
        mailConnection.getPassword(),
        mailConnection.getHost(),
        mailConnection.getPort(),
        "其他文件夹/test"
    );
    return IntegrationFlow
        .from(Mail.imapInboundAdapter(url)
                //.searchTermStrategy(this::fromAndNotSeenTerm)
                //.userFlag("testSIUserFlag")
                .simpleContent(true)
                .javaMailProperties(p -> {
//                      p.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//                      p.put("mail.imap.socketFactory.fallback", "false");
//                      p.put("mail.store.protocol", "imaps");
//                      p.put("mail.debug", "true");
                      mailConnection.getProperties().forEach(p::put);
                    }
                ),
            e -> e.autoStartup(true)
                .poller(p -> p.fixedDelay(1000)))
        .handle(new AbstractMessageHandler() {
          @Override
          protected void handleMessageInternal(Message<?> message) {
            log.info("{}", message.getHeaders().get(MailHeaders.SUBJECT));
          }
        })
        .get();
  }

  private IntegrationFlow sendMailFlow(MailProperties mailProperties) {
    return IntegrationFlow.from("sendMailChannel")
        .enrichHeaders(Mail.headers()
            .subjectFunction(m -> "foo")
            .from(mailProperties.getUsername())
            .toFunction(m -> new String[]{to, cc, bcc}))
        .handle(Mail.outboundAdapter(mailProperties.getHost())
                .port(mailProperties.getPort())
                .credentials(mailProperties.getUsername(), mailProperties.getPassword())
                .protocol(mailProperties.getProtocol()),
            e -> e.id("sendMailEndpoint"))
        .get();
  }

  @Import(value = {
      TestIntegrationConfig.class,
      MailConfiguration.class,
      MailSenderAutoConfiguration.class
  })
  @TestConfiguration
  public static class ContextConfig {

  }
}
