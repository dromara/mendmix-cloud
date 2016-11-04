#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-cache</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```
#### 特别说明
redis初始化过程使用到com.jeesuite.spring.InstanceFactory，因此需要在启动的时候初始化。
- web.xml
```
com.jeesuite.spring.web.ContextLoaderListener
替换
org.springframework.web.context.ContextLoaderListener
<listener>
	<listener-class>com.jeesuite.spring.web.ContextLoaderListener</listener-class>
</listener>
```
- 非web容器启动
ApplicationContext初始化完成后，调用InstanceFactory.setInstanceProvider(new SpringInstanceProvider(context));

#### 关于key的说明
- key约定格式：namespace.key前缀:实际值，如：（User.id:1001,User.all,User.type:1）
- namespace及对应一级缓存的cacheName，如：User.id:1001,User.all,User.type:1都属于cacheName[User]
- mybatis模块自动缓存的namespace即为实体类名，如:UserEntity

#### spring配置文件
```
  <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
		<property name="maxTotal" value="${redis.pool.maxTotal}" />
		<property name="maxIdle" value="${redis.pool.maxIdle}" />
		<property name="minIdle" value="${redis.pool.minIdle}" />
	</bean>

    <!--第一组缓存-->
	<bean id="defaultCache" class="com.jeesuite.cache.redis.JedisProviderFactoryBean">
		<property name="jedisPoolConfig" ref="jedisPoolConfig" />
		<!-- mode(standalone:单机模式，sentinel：哨兵模式(主从),cluster：集群模式，shard：分片模式) -->
		<property name="mode" value="standalone" />
		<!-- 多个用“,”隔开 -->
		<property name="servers" value="127.0.0.1:6379" />
		<property name="timeout" value="3000" />
		<!-- sentinel模式才需要该属性
		<property name="masterName" value="${redis.masterName}" />
		 -->
	</bean>

	<bean id="sessionCache"
		class="com.jeesuite.cache.redis.JedisProviderFactoryBean">
		<property name="group" value="session_cache" />
		<property name="jedisPoolConfig" ref="jedisPoolConfig" />
		<property name="mode" value="standalone" />
		<property name="servers" value="127.0.0.1:6380" />
		<property name="timeout" value="3000" />
	</bean> 

    <!-- 本地一级缓存支持 -->
    <bean id="level1CacheSupport" class="com.jeesuite.cache.local.Level1CacheSupport">
         <property name="enable" value="true" />
         <property name="servers" value="${redis.servers}" />
         <property name="maxSize" value="10000" />
         <property name="timeToLiveSeconds" value="300" />
         <!-- 需要本地缓存缓存组名。多个用,或;隔开 -->
         <property name="cacheNames">
            <value>User,City</value>
         </property>
    </bean>
```
配置文件说明
- group 即多套redis配置的支持。如果你的服务里面用到了多组缓存就必须指定组名，组名不能重复，不指定缺省为[default]。

#### 使用和基本语法
```
   //字符串
		RedisString redisString = new RedisString("User.id:1001");
		redisString.set("user1001",60);
		String value = redisString.get();
		
		redisString.getTtl();
		redisString.exists();
		redisString.setExpire(300);
		redisString.remove();
		
		//对象
		RedisObject redisObject = new RedisObject("User.id:1001");
		redisObject.set(new User(1001, "jack"));
		Object user = redisObject.get();
		redisObject.getTtl();
		redisObject.exists();
		redisObject.setExpire(300);
		redisObject.remove();
		
		//hash 
		RedisHashMap redisHashMap = new RedisHashMap("User.all");
		redisHashMap.set("1001", new User(1001, "jack"));
		redisHashMap.set("1002", new User(1002, "jack2"));
		
		Map<String, User> users = redisHashMap.get("1001","1002");
		users = redisHashMap.getAll();
		User one = redisHashMap.getOne("1001");
		
		redisHashMap.containsKey("1001");
		
		redisHashMap.remove();
		
		//指定缓存服务组名
		//new RedisObject(key, groupName)
		new RedisObject("User.id:1001", "session_cache");
```
其他还有：
- RedisList
- RedisNumber
- RedisSet
