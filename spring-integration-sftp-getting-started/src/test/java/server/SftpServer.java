package server;

import java.io.IOException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SftpServer {

  public static void main(String[] args) throws IOException {
    ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        "classpath:META-INF/spring/integration/SftpSampleCommon.xml");
    applicationContext.start();
    System.in.read();
  }
}
