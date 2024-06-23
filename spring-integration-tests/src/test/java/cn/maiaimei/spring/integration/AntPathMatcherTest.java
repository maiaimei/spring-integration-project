package cn.maiaimei.spring.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

/**
 * https://www.runoob.com/regexp/regexp-metachar.html
 */
public class AntPathMatcherTest {

  @Test
  public void testMatch() {
    AntPathMatcher matcher = new AntPathMatcher();

    assertTrue(matcher.match("*_TRANSACTION_*_*.json", "HKBOC_TRANSACTION_20240620_foo.json"));
    assertTrue(matcher.match("*_TRANSACTION_*_*.json", "HKBOC_TRANSACTION_20240620_01.json"));
    assertFalse(matcher.match("*_TRANSACTION_*_*.json", "HKBOC_TRANSACTION_20240620_01.json.writing"));

    assertFalse(matcher.match("{spring:\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json}", "HKBOC_TRANSACTION_20240620_foo.json"));
    assertTrue(matcher.match("{spring:\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json}", "HKBOC_TRANSACTION_20240620_01.json"));
    assertFalse(matcher.match("{spring:\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json}", "HKBOC_TRANSACTION_20240620_01.json.writing"));

    assertFalse(matcher.match("{spring:^\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json$}", "HKBOC_TRANSACTION_20240620_foo.json"));
    assertTrue(matcher.match("{spring:^\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json$}", "HKBOC_TRANSACTION_20240620_01.json"));
    assertFalse(matcher.match("{spring:^\\S+_TRANSACTION_[0-9]{8}_[0-9]+.json$}", "HKBOC_TRANSACTION_20240620_01.json.writing"));
  }
}
