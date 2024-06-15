package cn.maiaimei.spring.integration.config;

import cn.maiaimei.commons.lang.utils.FileUtils;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ChannelInterceptor;

@Slf4j
//@Configuration
public class SftpIntegrationConfig {

  @Bean
  public MessageChannel fileInputChannel() {
    final DirectChannel directChannel = new DirectChannel();
    directChannel.addInterceptor(fileReadWriteInterceptor());
    return directChannel;
  }

  @Bean
  @InboundChannelAdapter(
      channel = "fileInputChannel",
      poller = @Poller(cron = "* * * * * ?", maxMessagesPerPoll = "1")
  )
  public MessageSource<File> fileReadingMessageSource() {
    CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
    filter.addFilter(new SimplePatternFileListFilter("*.txt"));
    FileReadingMessageSource messageSource = new FileReadingMessageSource();
    messageSource.setDirectory(FileUtils.getFile("C:\\Users\\lenovo\\Desktop\\tmp\\input"));
    messageSource.setAutoCreateDirectory(Boolean.TRUE);
    messageSource.setFilter(filter);
    return messageSource;
  }

  @Bean
  @ServiceActivator(inputChannel = "fileInputChannel")
  public MessageHandler fileWritingMessageHandler() {
    FileWritingMessageHandler messageHandler = new FileWritingMessageHandler(
        FileUtils.getOrCreateDirectory("C:\\Users\\lenovo\\Desktop\\tmp\\output")
    );
    messageHandler.setAutoCreateDirectory(Boolean.TRUE);
    messageHandler.setDeleteSourceFiles(Boolean.TRUE);
    messageHandler.setExpectReply(Boolean.FALSE);
    // 设置当文件存在时的行为
    messageHandler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
    return messageHandler;
  }

  /**
   * https://docs.spring.io/spring-integration/reference/channel/interceptors.html
   */
  private ChannelInterceptor fileReadWriteInterceptor() {
    return new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        String filename = (String) message.getHeaders().get(FileHeaders.FILENAME);
        log.info("file [{}] is detected in input folder", filename);
        return message;
      }

      @Override
      public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent,
          Exception ex) {
        String filename = (String) message.getHeaders().get(FileHeaders.FILENAME);
        if (sent) {
          log.info("file [{}] has been moved to output folder", filename);
        } else {
          log.error(String.format("file [%s] failed to moved to output folder", filename), ex);
          String filepath = ((File) message.getHeaders()
              .get(FileHeaders.ORIGINAL_FILE)).getAbsolutePath();
          FileUtils.moveFile(filepath,
              "C:\\Users\\lenovo\\Desktop\\tmp\\output\\failed\\" + filename);
        }
      }
    };
  }
}
