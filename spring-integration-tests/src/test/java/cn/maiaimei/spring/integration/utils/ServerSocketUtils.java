package cn.maiaimei.spring.integration.utils;

import java.io.IOException;
import java.net.ServerSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerSocketUtils {

  public static int getRandomPort() {
    int randomPort = 0;
    // ServerSocket构造函数中的参数0指示操作系统自动分配一个随机的可用端口号。
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      randomPort = serverSocket.getLocalPort();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return randomPort;
  }
}
