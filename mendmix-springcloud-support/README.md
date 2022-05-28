#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-springboot-starter</artifactId>
	<version>1.1.3</version>
</dependency>
```

#### 注解定义
 - @EnableJeesuiteCache ：集成cache模块
 - @EnableJeesuiteMybatis ： 集成mybatis模块
 - @EnableJeesuiteSchedule ：集成schedule模块
 - @EnableJeesuiteKafkaConsumer ： 集成kafka consumer
 - @EnableJeesuiteKafkaProducer ： 集成kafka producer
 
#### 配置
注：=后面的为默认值，（）包含的为可选值。含义请参考具体模块。
```
--cache
jeesuite.cache.mode=standalone (standalone:单机模式，sentinel：哨兵模式(主从),cluster：集群模式，shard：分片模式)
jeesuite.cache.servers=
jeesuite.cache.password=
jeesuite.cache.database=0
jeesuite.cache.maxPoolSize=
jeesuite.cache.maxPoolIdle=
jeesuite.cache.minPoolIdle=
jeesuite.cache.maxPoolWaitMillis=
jeesuite.cache.level1.distributedMode=true
jeesuite.cache.level1.bcastServer=
jeesuite.cache.level1.password=
jeesuite.cache.level1.bcastScope=
jeesuite.cache.level1.cacheProvider=guavacache  (ehcache , guavacache)
jeesuite.cache.level1.maxCacheSize=5000
jeesuite.cache.level1.timeToLiveSeconds=300
jeesuite.cache.level1.cacheNames=

--mybatis
jeesuite.mybatis.dbType=MySQL
jeesuite.mybatis.crudDriver=mapper3
jeesuite.mybatis.cacheEnabled=false
jeesuite.mybatis.rwRouteEnabled=false
jeesuite.mybatis.dbShardEnabled=false
jeesuite.mybatis.paginationEnabled=true

--schedule
jeesuite.task.registryType=zookeeper
jeesuite.task.registryServers=
jeesuite.task.groupName=
jeesuite.task.scanPackages=
jeesuite.task.threadPoolSize=

--kafka
kafka.bootstrap.servers=
kafka.zkServers=
jeesuite.kafka.producer.defaultAsynSend=
jeesuite.kafka.producer.producerGroup=
jeesuite.kafka.producer.monitorZkServers=
jeesuite.kafka.producer.delayRetries=0
#其他kafka原生配置(kafka.producer.xxx)
kafka.producer.acks=1
kafka.producer.retries=1

jeesuite.kafka.consumer.independent
jeesuite.kafka.consumer.useNewAPI=false
jeesuite.kafka.consumer.processThreads=100
jeesuite.kafka.consumer.scanPackages=
#其他kafka原生配置(kafka.consumer.xxx)
kafka.consumer.group.id=
kafka.consumer.enable.auto.commit=

```

#### 使用

