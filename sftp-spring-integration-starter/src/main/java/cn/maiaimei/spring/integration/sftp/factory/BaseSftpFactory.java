package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.utils.StringUtils;
import cn.maiaimei.spring.integration.sftp.utils.PropertiesUtils;
import java.util.Map;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.remote.session.CachingSessionFactory;

public class BaseSftpFactory {

  protected Logger log;

  protected ApplicationContext applicationContext;

  protected Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap;

  public BaseSftpFactory() {
    this.log = LoggerFactory.getLogger(getClass());
  }

  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public void setSessionFactoryMap(
      Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap) {
    this.sessionFactoryMap = sessionFactoryMap;
  }

  protected String getCron(String cron, String configName) {
    if (StringUtils.hasText(cron)) {
      return cron;
    }
    return PropertiesUtils.getProperty(applicationContext, configName, String.class);
  }

  protected Long getMaxMessagesPerPoll(Long maxMessagesPerPoll, String configName) {
    if (maxMessagesPerPoll > 0) {
      return maxMessagesPerPoll;
    }
    return PropertiesUtils.getProperty(applicationContext, configName, Long.class);
  }

}
