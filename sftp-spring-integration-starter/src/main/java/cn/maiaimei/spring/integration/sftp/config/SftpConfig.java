package cn.maiaimei.spring.integration.sftp.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.util.Assert;

/**
 * SFTP configuration
 */
@Configuration
@EnableConfigurationProperties(SftpConnectionHolder.class)
public class SftpConfig {

  /**
   * Construct a {@link CachingSessionFactory} map by the given connections.
   *
   * @param sftpConnectionHolder the SFTP connections config holder
   * @return a {@link CachingSessionFactory} map
   */
  @Bean
  public Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap(
      SftpConnectionHolder sftpConnectionHolder) {
    Assert.notNull(sftpConnectionHolder, "sftpConnectionHolder must not be null");
    Assert.notEmpty(sftpConnectionHolder.getConnections(), "connections must not be null");
    Map<String, CachingSessionFactory<DirEntry>> sessionFactoryMap = new HashMap<>(
        sftpConnectionHolder.getConnections().size());
    sftpConnectionHolder.getConnections().forEach((schema, connection) -> {
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
    sessionFactory.setPrivateKey(conn.getPrivateKey());
    sessionFactory.setPrivateKeyPassphrase(conn.getPrivateKeyPassphrase());
    sessionFactory.setAllowUnknownKeys(Boolean.TRUE);
    return sessionFactory;
  }

}
