#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-cache</artifactId>
	<version>1.1.3</version>
</dependency>
```

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
		<property name="password" value="mypass" />
		<property name="database" value="1" />
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
```
配置文件说明
- mode:  standalone:单机模式，sentinel：哨兵模式(主从),cluster：集群模式，shard：分片模式
- servers:redis server列表，多个','隔开。sentinel模式要配置哨兵地址（敲黑板～～～～）
- group: 即多套redis配置的支持。如果你的服务里面用到了多组缓存就必须指定组名，组名不能重复，不指定缺省为[default]。
- database：默认为 0
- password： redis设置了密码就配置

开启本地一级缓存（集群下一级缓存自动更新）
```
    <bean id="level1CacheSupport" class="com.jeesuite.cache.local.Level1CacheSupport">
         <property name="distributedMode" value="true" />
         <property name="bcastServer" value="${redis.servers}" />
         <property name="password" value="mypass" />
         <property name="bcastScope" value="demo" />
         <property name="cacheProvider">
            <!-- <bean class="com.jeesuite.cache.local.GuavaLevel1CacheProvider">
               <property name="maxSize" value="10000" />
               <property name="timeToLiveSeconds" value="300" />
            </bean> -->
            <bean class="com.jeesuite.cache.local.EhCacheLevel1CacheProvider">
               <property name="ehcacheName" value="level1Cache" />
            </bean>
         </property>
         <!-- 需要本地缓存缓存组名。多个用,或;隔开 -->
         <property name="cacheNames">
            <value>UserEntity,CityEntity</value>
         </property>
    </bean>
```
配置文件说明
- distributedMode：是否开启分布式模式、默认（true），不开启的话一级缓存更新就不会广播通知了。
- bcastServer：广播服务器地址，目前使用redis订阅发布机制广播、所以配置redis地址即可。
- password：bcastServer的redis有密码则配置
- bcastScope：一般配置你的应用名即可。主要防止多个应用使用同一个缓存的情况，出现交叉订阅缓存同步更新情况。
- cacheProvider：本地缓存实现。目前支持guava cache和ehcache。默认guava cache
- cacheNames：需要开启一级缓存缓存组，不配置实际上一级缓存就不生效。按目前自动缓存key规则（namespace.key:value）,namespace即为cacheName

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
- RedisSortSet


如果这些封装不满足你的需求这些你需要用其他的可以直接通过一下方式调用jedis原生API：
```
JedisCommands commands = JedisProviderFactory.getJedisCommands(null);
try {			
	commands.zadd(cacheGroup, score, key);
	commands.pexpire(cacheGroup, expireSeconds * 1000);
} finally{
	JedisProviderFactory.getJedisProvider(null).release();
}
```
