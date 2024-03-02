package cn.maiaimei.samples.utils;

import java.util.Objects;
import org.springframework.util.StringUtils;

public class StringUtil extends StringUtils {

  public static String concat(String delimiter, String... values) {
    if (Objects.nonNull(values)) {
      StringBuilder builder = new StringBuilder();
      final int length = values.length;
      for (int i = 0; i < length; i++) {
        String value = values[i];
        if (hasText(value)) {
          builder.append(value);
          if (i != length - 1) {
            builder.append(delimiter);
          }
        }
      }
      return builder.toString();
    }
    return null;
  }

}
