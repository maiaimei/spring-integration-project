package cn.maiaimei.spring.integration;

import cn.maiaimei.commons.lang.utils.StringUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

@Slf4j
public abstract class FileTestSupport {

  @TempDir
  public static Path temporaryFolder;

  protected static File remoteTemporaryFolder;

  protected static File localTemporaryFolder;

  public static File getRemoteTemporaryFolder() {
    return remoteTemporaryFolder;
  }

  @BeforeEach
  public void setUp(TestInfo info) {
    createRemoteTempFolder(info);
    createLocalTempFolder(info);
    doSetUp();
  }

  @AfterEach
  public void tearDown() {
    doTearDown();
  }

  protected void doSetUp() {
    // give subclass a chance to setup environment
  }

  protected void doTearDown() {
    // give subclass a chance to setup environment
  }

  protected static void createFolders(TestInfo info) {
    createRemoteTempFolder(info);
    createLocalTempFolder(info);
  }

  protected File createRemoteFolder(String... children) {
    final String child = StringUtils.concat(File.separator, children);
    File file = new File(remoteTemporaryFolder, child);
    file.mkdirs();
    return file;
  }

  protected File createLocalFolder(String... children) {
    final String child = StringUtils.concat(File.separator, children);
    File file = new File(localTemporaryFolder, child);
    file.mkdirs();
    return file;
  }

  private static void createRemoteTempFolder(TestInfo info) {
    if (remoteTemporaryFolder == null) {
      remoteTemporaryFolder = new File(
          temporaryFolder.toFile().getAbsolutePath() + File.separator + "remote");
      remoteTemporaryFolder.mkdirs();
      log.info("remote temporary folder: {}", remoteTemporaryFolder.getAbsolutePath());
    } else {
      recursiveDelete(remoteTemporaryFolder, info);
    }
  }

  private static void createLocalTempFolder(TestInfo info) {
    if (localTemporaryFolder == null) {
      localTemporaryFolder = new File(
          temporaryFolder.toFile().getAbsolutePath() + File.separator + "local");
      localTemporaryFolder.mkdirs();
      log.info("local temporary folder: {}", localTemporaryFolder.getAbsolutePath());
    } else {
      recursiveDelete(localTemporaryFolder, info);
    }
  }

  private static void recursiveDelete(File file, TestInfo info) {
    if (file != null && file.exists()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File fyle : files) {
          log.info("Deleting: " + fyle + " in " + info.getDisplayName());
          if (fyle.isDirectory()) {
            recursiveDelete(fyle, info);
          } else {
            if (!fyle.delete()) {
              log.error("Couldn't delete: " + fyle + " in " + info.getDisplayName());
            }
          }
        }
      }
      log.info("Deleting: " + file + " in " + info.getDisplayName());
      if (!file.delete()) {
        log.error("Couldn't delete: " + file + " in " + info.getDisplayName());
        if (file.isDirectory()) {
          log.error("Contents: " + Arrays.toString(file.listFiles()));
        }
      }
    }
  }

}
