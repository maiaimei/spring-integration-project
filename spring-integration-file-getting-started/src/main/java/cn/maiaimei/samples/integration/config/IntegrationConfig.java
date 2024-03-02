package cn.maiaimei.samples.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.PollableChannel;

@Configuration
@EnableIntegration
@IntegrationComponentScan
public class IntegrationConfig {

  @Bean("errorChannel")
  public PollableChannel errorChannel() {
    return new QueueChannel();
  }
}
