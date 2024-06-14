package cn.maiaimei.spring.integration.sftp.factory;

import cn.maiaimei.commons.lang.utils.FileUtils;
import cn.maiaimei.spring.integration.sftp.config.rule.BaseSftpInboundRule;
import java.io.File;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.util.Assert;

/**
 * SftpInboundFactory
 */
public class SftpInboundFactory extends BaseSftpFactory {


  public MessageSource<File> sftpInboundMessageSource(BaseSftpInboundRule rule) {
    final SftpInboundFileSynchronizer synchronizer = sftpInboundFileSynchronizer(rule);
    SftpInboundFileSynchronizingMessageSource source =
        new SftpInboundFileSynchronizingMessageSource(
            synchronizer);
    source.setLocalDirectory(FileUtils.getFile(rule.getLocal()));
    source.setAutoCreateLocalDirectory(Boolean.TRUE);
    if (rule.isAcceptOnce()) {
      source.setLocalFilter(new AcceptOnceFileListFilter<>());
    }
    return source;
  }

  public SftpInboundFileSynchronizer sftpInboundFileSynchronizer(BaseSftpInboundRule rule) {
    SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(
        sessionFactoryMap.get(rule.getSchema()));
    fileSynchronizer.setDeleteRemoteFiles(Boolean.TRUE);
    fileSynchronizer.setRemoteDirectory(rule.getRemoteSource());
    fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter(rule.getPattern()));
    return fileSynchronizer;
  }

  /**
   * Construct a {@link RemoteFileTemplate} instance by the given rule.
   *
   * @param rule the rule to construct instance
   * @return a {@link RemoteFileTemplate} instance
   */
  private RemoteFileTemplate<DirEntry> template(BaseSftpInboundRule rule) {
    RemoteFileTemplate<DirEntry> template = new RemoteFileTemplate<>(
        sessionFactoryMap.get(rule.getSchema()));
    template.setRemoteDirectoryExpression(new LiteralExpression(rule.getRemoteSource()));
    template.setAutoCreateDirectory(Boolean.TRUE);
    template.setUseTemporaryFileName(Boolean.TRUE);
    template.setBeanFactory(applicationContext);
    // must invoke method "afterPropertiesSet", 
    // otherwise will throw java.lang.RuntimeException: No beanFactory
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Validate the given rule
   *
   * @param rule the rule to validate
   */
  private void validateRule(BaseSftpInboundRule rule) {
    Assert.hasText(rule.getSchema(), "schema must be configured");
    Assert.hasText(rule.getName(), "name must be configured");
    Assert.hasText(rule.getPattern(), "pattern must be configured");
    Assert.hasText(rule.getLocal(), "local must be configured");
    Assert.hasText(rule.getRemoteSource(), "remoteSource must be configured");
    Assert.hasText(rule.getRemoteTemp(), "remoteSource must be configured");
    Assert.hasText(rule.getRemoteArchive(), "remoteArchive must be configured");
  }
}
