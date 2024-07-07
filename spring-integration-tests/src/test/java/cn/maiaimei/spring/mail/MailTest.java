package cn.maiaimei.spring.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * https://docs.spring.io/spring-boot/reference/io/email.html
 * <p>
 * https://docs.spring.io/spring-framework/reference/integration/email.html
 * <p>
 * https://docs.spring.io/spring-boot/reference/features/external-config.html
 */
@Slf4j
@ActiveProfiles({"qq"})
//@PropertySource(value = "classpath:application-test.yml")
//@TestPropertySource(locations = "classpath:application-test.yml")
//@PropertySource(value = "file:/config/application-test.yml")
@ContextConfiguration(
    classes = {MailTest.ContextConfig.class},
    initializers = ConfigDataApplicationContextInitializer.class
)
@ExtendWith(SpringExtension.class)
public class MailTest {

  @Value("${test.mail.from:unknown}")
  private String from;

  @Value("${test.mail.to:unknown}")
  private String to;

  @Value("${test.mail.cc:unknown}")
  private String cc;

  @Value("${test.mail.bcc:unknown}")
  private String bcc;

  @Autowired
  private MailProperties mailProperties;

  @Autowired
  private JavaMailSender javaMailSender;

  @Test
  public void testPropertySource() {
    // TODO: @PropertySource和@TestPropertySource用法
    log.info("{}", from);
    log.info("{}", mailProperties.getUsername());
  }

  @Test
  public void testSendSimpleMail() {
    SimpleMailMessage simpleMessage = new SimpleMailMessage();
    simpleMessage.setSubject("Test Simple Mail 03");
    simpleMessage.setText("Test Simple Mail 03 body");
    simpleMessage.setFrom(mailProperties.getUsername());
    simpleMessage.setTo(mailProperties.getUsername());
    simpleMessage.setCc(cc);
    simpleMessage.setBcc(bcc);
    javaMailSender.send(simpleMessage);
  }

  @Test
  public void testSendMiscellaneousMail() throws MessagingException, IOException {
    final ClassPathResource classPathResource = new ClassPathResource("mail/test.txt");
    final File classPathResourceFile = classPathResource.getFile();

    final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, Boolean.TRUE);
    helper.setSubject("Test Miscellaneous Mail 03");
    helper.setText(
        "Test Miscellaneous Mail 03 body <p><img src=\"https://0img.chimelong"
            + ".com/backend-file/2024/06/26/105334_a6f7ce4e21784c0bb864c59d02e79137.jpg\" /></p>", Boolean.TRUE);
    helper.setFrom(mailProperties.getUsername());
    helper.setTo(to);
    //helper.setCc(InternetAddress.parse(cc, Boolean.TRUE));
    helper.setBcc(InternetAddress.parse(bcc, Boolean.FALSE));
    helper.addAttachment(classPathResourceFile.getName(), classPathResourceFile);
    helper.addAttachment(classPathResourceFile.getName().concat(".txt"), classPathResourceFile);
    javaMailSender.send(mimeMessage);
  }

  @Configuration
  @Import(MailSenderAutoConfiguration.class)
  public static class ContextConfig {

  }
}