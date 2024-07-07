# Spring

@ExtendWith(SpringExtension.class) 是 JUnit 5 中用于集成 Spring 环境的注解，提供了Spring单测的上下文环境, 会启动一个用于单测的Spring容器，完成单测中所需的bean对象的构建与自动注入。

@ActiveProfiles 激活指定环境配置。配置文件需要符合 application-\*.yml 或 application-\*.properties 命名规范且放在resources目录下。

@PropertySource 和 @ContextConfiguration 是Spring框架中用于进行单元测试的两个注解。

@PropertySource 或 @TestPropertySource 用于指定属性文件的位置，这样可以在测试过程中使用属性文件中定义的值。

@ImportResource

@ContextConfiguration 用于指定Spring上下文的配置类或定义XML文件的位置，以便在测试运行时创建应用程序上下文。

以下为SpringBoot项目读取配置文件优先级顺序：

1. config/application.properties（项目根目录的config目录下）

2. config/application.yml（项目根目录的config目录下）
3. application.properties（项目根目录下）

4. application.yml（项目根目录下）
5. src/test/resources/config/application.properties

6. src/test/resources/config/application.yml
7. src/test/resources/application.properties

8. src/test/resources/application.yml
9. src/main/resources/config/application.properties

10. src/main/resources/config/application.yml
11. src/main/resources/application.properties

12. src/main/resources/application.yml

# Junit

在非spring项目中：

* 在Junit5前，使用@RunWith(MockitoJUnitRunner.class)及@InjectMocks/@Mock/@Spy注解来配合写单元测试用例。

* 在Junit5中，使用@ExtendWith(MockitoExtension.class)及@InjectMocks/@Mock/@Spy注解来配合写单元测试用例。

在spring项目中：

* 在Junit5前，使用@RunWith(SpringJUnit4ClassRunner.class)
  及@InjectMocks/@MockBean/@SpyBean注解来配合写单元测试用例。

* 在Junit5中，使用@ExtendWith(SpringExtension.class)及@InjectMocks/@MockBean/@SpyBean注解来配合写单元测试用例。

# Apache MINA

Apache sshd是一个SSH协议的100%纯Java库，支持客户端和服务器。sshd库基于Apache MINA项目（可伸缩高性能的异步IO库）。

Apache MINA 是一个网络应用框架，用于开发高性能、高可靠性、高可伸缩性的网络服务器和客户端。

官方网站：http://mina.apache.org/sshd-project/documentation.html

# 知识扩展

如何确保文件传输数据完整性？

1. 通过文件名实现。在文件传输前，将文件重命名表示文件正在传输；在文件传输后，重命名文件以表示传输完毕。
2. 通过数据文件+标记文件实现。在文件传输前，创建一个标记文件表示文件正在传输；在文件传输后，删除标记文件以表示传输完毕。
   先上传数据文件1.DAT，上传完成后，再上传标记文件1.CTL。读取时，先判断是否存在标记文件1.CTL，如果存在则说明数据文件1.DAT文件数据完整。
3. 判断文件大小。如果文件的大小等于预期大小，则可以认为文件传输完成。
3. 判断文件大小。在一定时间间隔多次检查文件大小，如果文件大小没有变化，则可以认为文件传输完成。

