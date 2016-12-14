#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-scheduler</artifactId>
	<version>1.0.3</version>
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

#### 编写一个定时任务
```
public class DemoTask extends AbstractJob{

	int count = 1;
	@Override
	public void doJob(JobContext context) throws Exception {
		System.out.println("\n=============\nDemoTask1=====>"+count+"\n===============\n");
		Thread.sleep(RandomUtils.nextLong(1000, 2000));
		count++;
	}

	@Override
	public boolean parallelEnabled() {
	    // 分布式下开启并行计算返回true
		return false;
	}

}
```
#### 怎么写一个并行计算的逻辑
```
public class DemoParallelTask extends AbstractJob{

	int count = 1;
	@Override
	public void doJob(JobContext context) throws Exception {
		//首先加载所有要处理的数据，譬如所有需要处理的用户
		List<Long> userids = new ArrayList<Long>(Arrays.asList(1001L,2001L,1002L,2002L,1003L,2003L));//load all
		for (Long userId : userids) {
			//判断是否分配到当前节点执行
			if(!context.matchCurrentNode(userId)){
				System.out.println(">>>>>>not matchCurrentNode --ignore");
				continue;
			}
			// 处理具体业务逻辑
			System.out.println("<<<<<<<DemoTask2=====>"+count);
		}
		count++;
	}
	
	@Override
	public boolean parallelEnabled() {
		//开启并行计算
		return true;
	}

}
```
#### spring配置文件
```
 <!-- 注册中心（集群管理） -->
    <bean id="jobRegistry" class="com.jeesuite.scheduler.registry.ZkJobRegistry">
       <property name="zkServers" value="localhost:2181" />
    </bean>
   
	<bean class="com.jeesuite.scheduler.SchedulerFactoryBeanWrapper">
	    <!-- 确保每个应用groupName唯一 -->
	    <property name="groupName" value="demo" />
		<property name="schedulers">
			<list>
			    <!-- 以下配置每个任务列表 -->
				<bean class="com.jeesuite.test.sch.DemoTask">
					<property name="jobName" value="demoTask" />
					<property name="cronExpr" value="0 0/15 * * * ?" />
					<property name="jobRegistry" ref="jobRegistry" />
					<!-- 启动完成立即执行一次 -->
					<property name="executeOnStarted" value="true" />
				</bean>
				
				<bean class="com.jeesuite.test.sch.DemoParallelTask">
					<property name="jobName" value="demoParallelTask" />
					<property name="cronExpr" value="0 0/15 * * * ?" />
					<property name="jobRegistry" ref="jobRegistry" />
				</bean>
			</list>
		</property>
	</bean>

```
参数说明：
- groupName 要确保和其他项目唯一
- jobName 确保同一groupName下唯一
