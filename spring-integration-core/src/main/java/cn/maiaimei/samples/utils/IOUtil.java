package cn.maiaimei.samples.utils;

import cn.maiaimei.samples.constants.IntegrationConstants;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOUtil extends org.apache.commons.io.FileUtils {

  public static String getOrCreateTempDirectory() {
    try {
      Path tempDir = Files.createTempDirectory("temp");
      log.info("tmpdir {} created successfully", tempDir.toString());
      return tempDir.toString();
    } catch (IOException e) {
      log.error("tmpdir created failed", e);
    }
    final String tempDir = getTempDirectoryPath();
    log.info("use default tmpdir {}", tempDir);
    return tempDir;
  }

  public static File getOrCreateDirectory(final String... names) {
    final File file = getFile(names);
    final String path = file.getAbsolutePath();
    if (!file.exists()) {
      if (file.mkdirs()) {
        log.info("Directory {} created successfully", path);
      } else {
        log.error("Directory {} created failed", path);
      }
    }
    return file;
  }

  public static File getOrCreateFile(final String... names) {
    final File file = getFile(names);
    final String path = file.getAbsolutePath();
    if (!file.exists()) {
      try {
        if (file.createNewFile()) {
          log.info("File {} created successfully", path);
        } else {
          log.error("File {} created failed", path);
        }
      } catch (IOException e) {
        log.error(String.format("File %s created failed", path), e);
      }
    }
    return file;
  }

  public static String getPath(String... paths) {
    final String path = StringUtil.concat(File.separator, paths);
    return StringUtil.cleanPath(path);
  }

  public static void writeStringToFile(final String path, final String data) {
    writeStringToFile(path, data, StandardCharsets.UTF_8);
  }

  public static void writeStringToFile(final String path, final String data,
      final Charset charset) {
    String processingPath = path.concat(IntegrationConstants.FILE_SUFFIX_WRITING);
    try {
      final File processingFile = getOrCreateFile(processingPath);
      writeStringToFile(processingFile, data, charset, false);
      processingFile.renameTo(new File(path));
      log.info("File {} created successfully", path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<File> listFiles(String path) {
    final File file = new File(path);
    if (file.exists() && file.isDirectory()) {
      final File[] files = file.listFiles();
      if (Objects.nonNull(files)) {
        return List.of(files);
      }
    }
    return Collections.emptyList();
  }

  public boolean renameTo(String src, String dest) {
    final File srcFile = new File(src);
    final File destFile = new File(dest);
    return srcFile.renameTo(destFile);
  }

}
