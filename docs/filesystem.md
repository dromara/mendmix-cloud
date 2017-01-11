文件系统服务目前支持七牛和fastDFS集成，提供了工厂模式和spring注入两种集成方式。
#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-filesystem</artifactId>
	<version>1.0.0</version>
</dependency>

<!--七牛需要依赖-->
<dependency>
	<groupId>com.qiniu</groupId>
	<artifactId>qiniu-java-sdk</artifactId>
	<version>7.2.2</version>
</dependency>

<!--fastDFS需要依赖-->
<dependency>
  <groupId>io.netty</groupId>
  <artifactId>netty-all</artifactId>
  <version>4.0.34.Final</version>
</dependency>
```
#### 工厂模式
##### 配置文件,xx.properties

```
fs.groupNames=group1,group2
# 服务提供者可选值（fastDFS，qiniu）
fs.group1.provider=qiniu
fs.group1.accessKey=iqq3aa-ncqfdGGubCcS-N8EUV-qale2ezndnrtKS
fs.group1.secretKey=1RmdaMVjrjXkyRVPOmyMa6BzcdG5VDdF-SH_HUTe
fs.group1.urlprefix=http://7xq7jj.com1.z0.glb.clouddn.com
# group2 配置
fs.group2.provider=fastDFS
fs.group2.servers=192.168.1.101:22122,192.168.1.102:22122
.....
fs.group2.connectTimeout=3000
fs.group2.maxThreads=100
......
```

参数说明：
- groupNames：多个组用“,”隔开，对应七牛的`bucketName`

##### 用法

```
FSProvider provider = FSClientFactory.build("qiniu", "group1");
String url = provider.upload("test", null, new File("/Users/vakinge/logo.gif"));
```

spring方式

```
   <bean id="qiniuFSProvider" class="com.jeesuite.filesystem.spring.FSProviderSpringFacade">
        <property name="provider" value="qiniu" />
        <property name="groupName" value="demo1" />
        <property name="accessKey" value="iqq3aa-ncqfdGGubCcS-N8EUV-qale2ezndnrtKS" />
        <property name="secretKey" value="1RmdaMVjrjXkyRVPOmyMa6BzcdG5VDdF-SH_HUTe" />
        <property name="urlprefix" value="http://7xq7jj.com1.z0.glb.clouddn.com" />
    </bean>
    
    <bean id="fastDfsFSProvider" class="com.jeesuite.filesystem.spring.FSProviderSpringFacade">
        <property name="provider" value="fastDFS" />
        <property name="groupName" value="group1" />
        <property name="servers" value="120.24.185.100:22122" />
        <property name="urlprefix" value="http://120.24.185.100:81" />
        <property name="connectTimeout" value="3000" />
    </bean>
```
##### 用法

```
@Autowired
FSProviderSpringFacade provider;
String url = provider.upload("test", null, new File("/Users/vakinge/logo.gif"));
```