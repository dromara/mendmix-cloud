### 简介
配置中心客户端，支持传统的spring项目已经springboot项目接入，使用前需要部署配置中心后台jeesuite统一管理平台[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin) 

### 特性
- 部署简单、功能单一
- 无缝对接传统spring项目和springboot项目，零成本迁移
- 支持用户基于环境隔离
- 支持应用接入授权
- 支持敏感配置加密

### 如何使用
#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-confcenter-client</artifactId>
	<version>最新版本号</version>
</dependency>
```

#### spring boot项目接入,只需要在`application.properties` 增加以下配置即可

```
jeesuite.configcenter.appName=demo
jeesuite.configcenter.base.url=http://localhost:7777
jeesuite.configcenter.profile=dev
jeesuite.configcenter.version=0.0.0
```

#### spring项目接入
添加和配置confcenter.properties文件（放在classpath下，限定名称为`confcenter.properties`）
```
jeesuite.configcenter.appName=demo
jeesuite.configcenter.base.url=http://localhost:7777
jeesuite.configcenter.profile=dev
jeesuite.configcenter.version=0.0.0
```

增spring配置

```
<bean class="com.jeesuite.confcenter.spring.CCPropertyPlaceholderConfigurer">
	   <property name="remoteEnabled" value="true" />
	   <!-- 本地配置文件 -->
	   <!-- 
	   <property name="location">
         <value>classpath:/local/*.properties</value>
       </property>
        -->
	</bean>
```
如果开发阶段你需要使用本地配置只需要设置`remoteEnabled=false`,开启本地配置

##### 配置说明
 - `jeesuite.configcenter.appName` 不定义则默认读取`spring.application.name`
 - `jeesuite.configcenter.version` 不定义则默认为：0.0.0