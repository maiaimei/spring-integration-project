package cn.maiaimei.spring.integration.sftp.utils;

import java.util.Objects;
import org.springframework.context.ApplicationContext;

public final class PropertiesUtils {

  private PropertiesUtils() {
    throw new UnsupportedOperationException();
  }

  public static <T> T getProperty(ApplicationContext applicationContext, String key, Class<T> targetType) {
    final T configValue = applicationContext.getEnvironment().getProperty(key, targetType);
    if (Objects.isNull(configValue)) {
      throw new IllegalArgumentException("No config named " + key);
    }
    return configValue;
  }
}
