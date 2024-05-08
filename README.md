在非spring项目中：

* 在Junit5前，使用@RunWith(MockitoJUnitRunner.class)及@InjectMocks/@Mock/@Spy注解来配合写单元测试用例。

* 在Junit5中，使用@ExtendWith(MockitoExtension.class)及@InjectMocks/@Mock/@Spy注解来配合写单元测试用例。

在spring项目中：

* 在Junit5前，使用@RunWith(SpringJUnit4ClassRunner.class)
  及@InjectMocks/@MockBean/@SpyBean注解来配合写单元测试用例。

* 在Junit5中，使用@ExtendWith(SpringExtension.class)及@InjectMocks/@MockBean/@SpyBean注解来配合写单元测试用例。
