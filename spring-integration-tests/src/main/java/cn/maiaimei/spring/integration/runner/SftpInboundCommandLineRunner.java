package cn.maiaimei.spring.integration.runner;

import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import cn.maiaimei.spring.integration.sftp.factory.SftpInboundFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.stereotype.Component;

/**
 * SftpInboundCommandLineRunner
 */
@Component
public class SftpInboundCommandLineRunner implements CommandLineRunner {

  @Autowired
  private IntegrationFlowContext flowContext;

  @Autowired
  private SftpInboundFactory sftpInboundFactory;

  @Autowired
  private List<BaseSftpInboundRule> sftpInboundRules;

  @Override
  public void run(String... args) throws Exception {
    sftpInboundRules.forEach(rule -> {
      final IntegrationFlow flow = sftpInboundFactory.createSimpleSftpInboundFlow(rule);
      flowContext.registration(flow).register();
    });
  }
}
