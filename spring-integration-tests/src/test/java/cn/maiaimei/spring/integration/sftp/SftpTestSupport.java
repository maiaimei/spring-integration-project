package cn.maiaimei.spring.integration.sftp;

import cn.maiaimei.spring.integration.FileTestSupport;
import java.io.File;
import java.util.Collections;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.server.ApacheMinaSftpEventListener;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * Provides an embedded SFTP Server for test cases.
 */
public class SftpTestSupport extends FileTestSupport {

  private static SshServer server;

  private static final ApacheMinaSftpEventListener EVENT_LISTENER
      = new ApacheMinaSftpEventListener();

  private static final File HOST_KEY_FILE = new File(
      temporaryFolder + File.separator + "hostkey.ser");

  protected static final String SFTP_SERVER_NAME = "foo";

  protected static final String SFTP_SERVER_USER = "foo";

  protected static final String SFTP_SERVER_PASSWORD = "foo";

  protected static final String SFTP_SERVER_HOST = "localhost";

  protected static int SFTP_SERVER_PORT;

  @BeforeAll
  public static void createServer(TestInfo info) throws Exception {
    createRemoteTempFolder(info);
    createLocalTempFolder(info);
    server = SshServer.setUpDefaultServer();
    server.setPasswordAuthenticator((username, password, session) -> true);
    server.setPort(0);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(HOST_KEY_FILE.toPath()));
    SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory();
    EVENT_LISTENER.setApplicationEventPublisher((ev) -> {
      // no-op
    });
    sftpFactory.addSftpEventListener(EVENT_LISTENER);
    server.setSubsystemFactories(Collections.singletonList(sftpFactory));
    server.setFileSystemFactory(new VirtualFileSystemFactory(getRemoteTemporaryFolder().toPath()));
    server.start();
    SFTP_SERVER_PORT = server.getPort();
  }

  public static SessionFactory<SftpClient.DirEntry> sessionFactory() {
    DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(Boolean.FALSE);
    factory.setHost(SFTP_SERVER_HOST);
    factory.setPort(SFTP_SERVER_PORT);
    factory.setUser(SFTP_SERVER_USER);
    factory.setPassword(SFTP_SERVER_PASSWORD);
    factory.setAllowUnknownKeys(Boolean.TRUE);
    CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(
        factory);
    cachingSessionFactory.setTestSession(Boolean.TRUE);
    return cachingSessionFactory;
  }

  public static ApacheMinaSftpEventListener eventListener() {
    return EVENT_LISTENER;
  }

  @AfterAll
  public static void stopServer() throws Exception {
    server.stop();
    if (HOST_KEY_FILE.exists()) {
      HOST_KEY_FILE.delete();
    }
  }

}
