package cn.maiaimei.spring.integration.sftp.config;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SFTP configuration
 */
@Configuration
public class SftpConfig {

  @Bean
  @ConfigurationProperties(prefix = "sftp")
  public SftpConnectionHolder defaultSftpConnectionHolder() {
    return new SftpConnectionHolder();
  }

  @Bean
  @ConfigurationProperties(prefix = "sftp.inbound")
  public SftpConnectionHolder inboundSftpConnectionHolder() {
    return new SftpConnectionHolder();
  }

  @Bean
  @ConfigurationProperties(prefix = "sftp.outbound")
  public SftpConnectionHolder outboundSftpConnectionHolder() {
    return new SftpConnectionHolder();
  }

  @Bean
  public SftpConnectionHolder sftpConnectionHolder() {
    SftpConnectionHolder defaultSftpConnectionHolder = defaultSftpConnectionHolder();
    SftpConnectionHolder inboundSftpConnectionHolder = inboundSftpConnectionHolder();
    SftpConnectionHolder outboundSftpConnectionHolder = outboundSftpConnectionHolder();
    final SftpConnectionHolder sftpConnectionHolder = new SftpConnectionHolder();
    sftpConnectionHolder.setConnections(Maps.newHashMap());
    if (Objects.nonNull(defaultSftpConnectionHolder)
        && !CollectionUtils.isEmpty(defaultSftpConnectionHolder.getConnections())) {
      sftpConnectionHolder.getConnections().putAll(defaultSftpConnectionHolder.getConnections());
    }
    if (Objects.nonNull(inboundSftpConnectionHolder)
        && !CollectionUtils.isEmpty(inboundSftpConnectionHolder.getConnections())) {
      sftpConnectionHolder.getConnections().putAll(inboundSftpConnectionHolder.getConnections());
    }
    if (Objects.nonNull(outboundSftpConnectionHolder)
        && !CollectionUtils.isEmpty(outboundSftpConnectionHolder.getConnections())) {
      sftpConnectionHolder.getConnections().putAll(outboundSftpConnectionHolder.getConnections());
    }
    return sftpConnectionHolder;
  }

  /**
   * Construct a {@link CachingSessionFactory} map by the given connections.
   *
   * @param sftpConnectionHolder the SFTP connections config holder
   * @return a {@link CachingSessionFactory} map
   */
  @Bean
  public Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap(
      @Autowired @Qualifier("sftpConnectionHolder") SftpConnectionHolder sftpConnectionHolder) {
    Assert.notNull(sftpConnectionHolder, "sftpConnectionHolder must not be null");
    Assert.notEmpty(sftpConnectionHolder.getConnections(), "sftp connections must not be null");
    Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap = new HashMap<>(
        sftpConnectionHolder.getConnections().size());
    sftpConnectionHolder.getConnections().forEach((schema, connection) -> {
      validateSftpConnection(connection);
      sessionFactoryMap.put(schema, cachingSessionFactory(connection));
    });
    return sessionFactoryMap;
  }

  /**
   * Construct a {@link CachingSessionFactory} instance by the given connection.
   *
   * @param conn the connection to use
   * @return a {@link CachingSessionFactory} instance
   */
  private CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory(SftpConnection conn) {
    CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory =
        new CachingSessionFactory<>(defaultSftpSessionFactory(conn));
    if (conn.getPoolSize() > 0) {
      cachingSessionFactory.setPoolSize(conn.getPoolSize());
    }
    if (conn.getWaitTimeout() > 0) {
      cachingSessionFactory.setSessionWaitTimeout(conn.getWaitTimeout());
    }
    if (conn.isTestSession()) {
      cachingSessionFactory.setTestSession(Boolean.TRUE);
    }
    return cachingSessionFactory;
  }

  /**
   * Construct a {@link DefaultSftpSessionFactory} instance by the given connection.
   *
   * @param conn the connection to use
   * @return a {@link DefaultSftpSessionFactory} instance
   */
  private DefaultSftpSessionFactory defaultSftpSessionFactory(SftpConnection conn) {
    DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
    sessionFactory.setHost(conn.getHost());
    sessionFactory.setPort(conn.getPort());
    sessionFactory.setUser(conn.getUser());
    if (Objects.nonNull(conn.getPrivateKey())) {
      sessionFactory.setPrivateKey(conn.getPrivateKey());
    } else if (StringUtils.hasText(conn.getPassword())) {
      sessionFactory.setPassword(conn.getPassword());
    }
    if (StringUtils.hasText(conn.getPrivateKeyPassphrase())) {
      sessionFactory.setPrivateKeyPassphrase(conn.getPrivateKeyPassphrase());
    }
    sessionFactory.setAllowUnknownKeys(Boolean.TRUE);
    return sessionFactory;
  }

  private void validateSftpConnection(SftpConnection conn) {
    Assert.hasText(conn.getHost(), "host must be configured");
    Assert.isTrue(conn.getPort() > 0 && conn.getPort() < 65535,
        "port must be assigned a unique number ranging from 0 to 65535");
    Assert.hasText(conn.getUser(), "user must be configured");
    Assert.isTrue(
        (Objects.nonNull(conn.getPrivateKey()) || StringUtils.hasText(conn.getPassword())),
        "password or private key must be configured");
  }

}
