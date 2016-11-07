#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-confcenter</artifactId>
	<version>1.0.0</version>
</dependency>
```
#### 添加和配置confcenter.properties文件（放在classpath下，限定名称为confcenter.properties）
```
confcenter.url=http://localhost:8080/confcenter/download
app.name=jeesuite-demo
app.env=dev
app.version=0.0.0
#应用自身的配置
app.config.files=kafka.properties,mysql.properties,redis.properties
#全局配置
global.config.files=sms.properties,email.properties
```
#### spring的配置
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
如果开发阶段你需要使用本地配置只需要设置remoteEnabled=false,开启本地配置

#### 远程配置文件管理
详见 [jeesuite管理平台说明文档](./admin.md)
