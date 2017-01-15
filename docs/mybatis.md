#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-mybatis</artifactId>
	<version>1.0.5</version>
</dependency>
```
#### 一些说明
##### 代码生成及其自动crud

已实现方法
- getByKey
- insert
- insertSelective
- updateByKey
- updateByKeySelective
- deleteByKey
- selectAll

### 特别说明：
已经无缝对接优秀的mybatis增强框架 [Mapper](http://git.oschina.net/free/Mapper) ，该框架功能强大，该部分推荐集成Mapper框架。

---
##### 其他功能说明
* 读写分离
  1. 根据执行sql类型自动路由master/slave库
  2. 支持基于properties文件动态扩展slave节点
  3. 支持@Transactional强制使用master库查询
* 自动缓存
  1. 支持所有mapper自定义查询方法自动缓存管理（自动缓存、自动更新）
  2. 支持通过@CacheEvictCascade注解，级联缓存更新
  3. 支持通过EntityCacheHelper手动写入/更新缓存并自动纳入自动管理体系
* 分库路由
  1. 只适用基本的分库路由，不支持跨库join等，未经过严格测试也没上过生产系统（只当个研究用）。

#### 配置
```
#mysql global config
db.group.size=1
db.shard.size=1000
db.driverClass=com.mysql.jdbc.Driver
db.initialSize=2
db.minIdle=1
db.maxActive=10
db.maxWait=60000
db.timeBetweenEvictionRunsMillis=60000
db.minEvictableIdleTimeMillis=300000
db.testOnBorrow=false
db.testOnReturn=false

#master
master.db.url=jdbc:mysql://localhost:3306/demo_db?seUnicode=true&amp;characterEncoding=UTF-8
master.db.username=root
master.db.password=123456
master.db.initialSize=2
master.db.minIdle=2
master.db.maxActive=20

#slave ....
slave1.db.url=jdbc:mysql://localhost:3306/demo_db2?seUnicode=true&amp;characterEncoding=UTF-8
slave1.db.username=root
slave1.db.password=123456

#slave2 ....
slave2.db.url=jdbc:mysql://localhost:3306/demo_db3?seUnicode=true&amp;characterEncoding=UTF-8
slave2.db.username=root
slave2.db.password=123456

#如果你要使用分库功能
#master
group1.master.db.url=jdbc:mysql://localhost:3306/demo_db?seUnicode=true&amp;characterEncoding=UTF-8
group1.master.db.username=root
group1.master.db.password=123456
group1.master.db.initialSize=2
group1.master.db.minIdle=2
group1.master.db.maxActive=20

#slave ....
group1.slave1.db.url=jdbc:mysql://localhost:3306/demo_db2?seUnicode=true&amp;characterEncoding=UTF-8
group1.slave1.db.username=root
group1.slave1.db.password=123456
```
说明
- `slave`序列从1开始,`group`序列从0开始(第一组group0默认省略，slave1~n,group1~n依次递增) 
- 每个数据源的配置会覆盖全局配置
- `db.group.size` 与 `db.shard.size` 都是分库用到配置，没有用到分库可以不配置



#####  spring配置
```
    <!--除了datasource配置外，其他都与原生配置一样-->
    <bean id="routeDataSource" class="com.jeesuite.mybatis.datasource.MutiRouteDataSource">
       <!-- 
       如果你的数据库配置文件内容自定义了加解密规则，请自行重写ConfigReader
       <property name="configReader">
           <bean class="com.jeesuite.mybatis.datasource.MyConfigReader" />
       </property>
        -->
    </bean>

	<bean id="transactionTemplate"
		class="org.springframework.transaction.support.TransactionTemplate">
		<property name="transactionManager" ref="transactionManager" />
		<property name="isolationLevelName" value="ISOLATION_DEFAULT" />
		<property name="propagationBehaviorName" value="PROPAGATION_REQUIRED" />
	</bean>

	<bean id="transactionManager"
		class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="dataSource" ref="routeDataSource" />
	</bean>

	<bean id="routeSqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
		<property name="configLocation" value="classpath:mybatis-configuration.xml" />
		<property name="mapperLocations" value="classpath:mapper/*Mapper.xml" /> 
		<property name="dataSource" ref="routeDataSource" />
		<property name="typeAliasesPackage" value="com.jeesuite.demo.dao.entity" />
		<property name="plugins">
            <array>
                <bean class="com.jeesuite.mybatis.plugin.JeesuiteMybatisPluginContext">
                    <!--可选值:default(默认),mapper3(集成mapper框架)-->
					<property name="crudDriver" value="default" />
				    <property name="mapperLocations" value="classpath:mapper/*Mapper.xml" />
				    <!--
				      可选值：cache(自动缓存),rwRoute(读写分离),dbShard(分库路由)
				    -->
				    <property name="interceptorHandlers" value="cache" />
				</bean> 
            </array>
        </property>
	</bean>
	<!--集成Mapper框架的配置,此处修改为tk.mybatis.spring.mapper.MapperScannerConfigurer-->
	<bean class="com.jeesuite.mybatis.spring.MapperScannerConfigurer">
        <property name="sqlSessionFactoryBeanName" value="routeSqlSessionFactory" />
		<property name="basePackage" value="com.jeesuite.demo.dao.mapper" />
    </bean>
```
默认使用jeesuite-cache模块作为自动缓存支持，如果你需要使用`spring-data-redis`或者自定义，可以按如下配置
```
<bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
	<property name="connectionFactory" ref="jedisConnectionFactory" />
    ....
</bean>

<bean id="stringRedisTemplate" class="org.springframework.data.redis.core.StringRedisTemplate">
  <property name="connectionFactory" ref="jedisConnectionFactory" />
</bean>

<!-- 使用spring-data-redis作为mybatis自动缓存提供者 -->
<bean class="com.jeesuite.mybatis.plugin.cache.provider.SpringRedisProvider">
	<property name="redisTemplate"  ref="redisTemplate"/>
	<property name="stringRedisTemplate" ref="stringRedisTemplate" />
</bean>
```
另外需要在自己项目加入spring-data-redis依赖

#### 如何使用自动缓存
只需要在mapper接口增加@com.jeesuite.mybatis.plugin.cache.annotation.Cache标注
```
public interface UserEntityMapper extends BaseMapper<UserEntity> {
	
	@Cache
	public UserEntity findByMobile(@Param("mobile") String mobile);
	
	@Cache
	List<String> findByType(@Param("type") int type);
}
```
配置完成即可，查询会自动缓存、更新会自动更新。


#### 一些其他说明
##### 开启了自动缓存数据库事务失败写入缓存如何处理？
- 在事务失败后调用CacheHandler.rollbackCache(),通常在业务的拦截器统一处理
##### 自动缓存的实体如何更新？
- 首先查询出实体对象，UserEntity entity = mapper.get...(id);
- 然后通过mapper.updateSelective(entity);

如果不全量更新会导致缓存的内容是有部分字段，因为update后会那最新的entity更新缓存。
##### 如果手动更新或写入实体缓存？
提供EntityCacheHelper方法，在代码中手动写入缓存也将纳入自动缓存管理，无需担心缓存更新问题。

```
/**
	 * 查询并缓存结果
	 * @param entityClass 实体类class (用户组装实际的缓存key)
	 * @param key 缓存的key（和entityClass一起组成真实的缓存key。<br>如entityClass=UserEntity.class,key=findlist，实际的key为：UserEntity.findlist）
	 * @param expireSeconds 过期时间，单位：秒
	 * @param dataCaller 缓存不存在数据加载源
	 * @return
	 */
	public static <T> T queryTryCache(Class<? extends BaseEntity> entityClass,String key,long expireSeconds,Callable<T> dataCaller)
```

```
//生成的缓存key为：UserEntity.findByStatus:2
EntityCacheHelper.queryTryCache(UserEntity.class, "findByStatus:2", new Callable<List<UserEntity>>() {
	public List<UserEntity> call() throws Exception {
	  //查询语句
	  List<UserEntity> entitys = mapper.findByStatus((short)2);
	  return entitys;
   }
});
```


