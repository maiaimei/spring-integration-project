# Spring

@ExtendWith(SpringExtension.class)提供了Spring单测的上下文环境, 会启动一个用于单测的Spring容器,
完成单测中所需的bean对象的构建与自动注入。

@PropertySource 和 @ContextConfiguration 是Spring框架中用于进行单元测试的两个注解。

@PropertySource 用于指定属性文件的位置，这样可以在测试过程中使用属性文件中定义的值。

@ContextConfiguration 用于指定Spring上下文的配置类或定义XML文件的位置，以便在测试运行时创建应用程序上下文。

@TestPropertySource

@ImportResource

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
1. 通过数据文件+标识文件实现。数据文件与标识文件同名，后缀不同。
   先上传数据文件1.DAT，上传完成后，再上传标识文件1.CTL。读取时，先判断是否存在标识文件1.CTL，如果存在则说明数据文件1.DAT文件数据完整。
2. 判断文件大小。如果文件的大小等于预期大小，则可以认为文件传输完成。
2. 添加文件后缀。文件传输完毕，重命名文件（去掉后缀）。

