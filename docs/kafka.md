- 基于kafka版本kafka_2.10-0.10.0.1
#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-kafka</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```
---

#### 开发一个producer
###### 首先在spring注册，添加以下配置文件即可
```
<bean id="topicProducerProvider" class="com.jeesuite.kafka.spring.TopicProducerSpringProvider">
        <!-- 默认是否异步发送 -->
        <property name="defaultAsynSend" value="true" />
        <!-- 标识同一集群，监控需要 -->
        <property name="producerGroup" value="demo" />
        <property name="configs">
            <!-- 以下参数参考:http://kafka.apache.org/documentation.html#producerconfigs -->
            <props>
                <prop key="bootstrap.servers" >${kafka.servers}</prop>
                <prop key="acks">1</prop>
                <prop key="retries">1</prop>
            </props>
        </property>
    </bean>
```

###### 发送一条消息
```
@Autowired
private TopicProducerSpringProvider topicProducer;
	
public void testPublish() {
    //默认模式（异步/ 同步）发送
    topicProducer.publish("demo-topic", new DefaultMessage("hello,man"));
	//异步发送
	topicProducer.publish("demo-topic", new DefaultMessage("hello,man"),true);
}
```
###### 关于DefaultMessage
```
DefaultMessage msg = new DefaultMessage("hello,man")
		            .header("headerkey1", "headerval1")//写入header信息
		            .header("headerkey1", "headerval1")//写入header信息
		            .partitionFactor(1000) //分区因子，譬如userId＝1000的将发送到同一分区、从而发送到消费者的同一节点(有状态)
		            .consumerAck(true);// 已消费回执(未发布)
```
#### 开发一个consumer
###### 首先在spring注册，添加以下配置文件即可
```
<bean  id="topicConsumerProvider" class="com.jeesuite.kafka.spring.TopicConsumerSpringProvider">
        <!-- 如果为false启动时将阻塞主线程，如zk连接超时就报错 -->
        <property name="independent" value="false" />
        <!-- 使用新版API开关,新版未在线上运行过，线上暂用老版API吧 -->
        <property name="useNewAPI" value="false" />
        <!-- 默认最大处理线程，可以配置大一下空闲会自动回收 -->
        <property name="processThreads" value="100" />
        <property name="configs">
            <!--参考 http://kafka.apache.org/documentation.html#newconsumerconfigs -->
            <props>
               <!-- 老版 consumerAPI 才需要zookeeper.connect -->
               <prop key="zookeeper.connect" >${kafka.zkServers}</prop>
                <prop key="bootstrap.servers" >${kafka.servers}</prop>
                <!-- 同一个组对同一个消息只会消费一次，所以不同业务使用不同的group.id -->
                <prop key="group.id">kafka-demo2</prop>
                <prop key="enable.auto.commit">true</prop>
                <!-- old api可选值：smallest，largest； 新版api可选值：earliest，largest -->
                <prop key="auto.offset.reset">smallest</prop>
                <!-- 131072 = 128kb -->
                <prop key="max.partition.fetch.bytes">131072</prop>
            </props>
        </property>
        <property name="topicHandlers" >
            <map>
              <!-- 指定消息监听器 -->
              <entry key="demo-topic">
                 <bean class="com.jeesuite.test.DemoMessageHandler" />
              </entry>
              <entry key="demo2-topic">
                 <bean class="com.jeesuite.test.Demo2MessageHandler" />
              </entry>
            </map>
        </property>
    </bean>
```
###### 编写一个对应的消息处理器
```
public class DemoMessageHandler implements MessageHandler {

	@Override
	public void p1Process(DefaultMessage message) {
		//第一阶段处理是同步处理，即在fetch线程处理
	}

	@Override
	public void p2Process(DefaultMessage message) {
		//第二阶段处理是异步处理，在处理线程池排队处理
		Serializable body = message.getBody();
		System.out.println("DemoMessageHandler process message:" + body);
		try {Thread.sleep(100);} catch (Exception e) {}
	}


	@Override
	public boolean onProcessError(DefaultMessage message) {
		System.out.println("ignore error message : "+message);
		return true;
	}

}
```
MessageHandler的一些说明
- 两阶段处理的同一份数据、一般情况第二阶段处理即可。
- 如果要触发重试机制请不要try catch异常
- onProcessError 返回true表示业务自身处理错误，否则框架会没30秒重新执行一次，最多重试3次