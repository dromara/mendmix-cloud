#### 添加依赖
```
<dependency>
	<groupId>com.jeesuite</groupId>
	<artifactId>jeesuite-scheduler</artifactId>
	<version>1.1.0</version>
</dependency>
```

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
	    <!--  -->
	    <property name="configPersistHandler">
	       <bean class="com.jeesuite.test.sch.DbConfigPersistHandler" />
	    </property>
	    <property name="registry" ref="jobRegistry" />
		<property name="schedulers">
			<list>
			    <!-- 以下配置每个任务列表 -->
				<bean class="com.jeesuite.test.sch.DemoTask">
					<property name="jobName" value="demoTask" />
					<property name="retries" value="1" />
					<property name="cronExpr" value="0/30 * * * * ?" />
					<!-- 启动完成立即执行一次 -->
					<property name="executeOnStarted" value="false" />
				</bean>
				
				<bean class="com.jeesuite.test.sch.DemoTask2">
					<property name="jobName" value="demoTask2" />
					<property name="cronExpr" value="0/15 * * * * ?" />
				</bean>
				
				<!-- <bean class="com.jeesuite.test.sch.DemoParallelTask">
					<property name="jobName" value="demoParallelTask" />
					<property name="cronExpr" value="0 0/15 * * * ?" />
				</bean> -->
			</list>
		</property>
	</bean>

```
全局参数说明：
- `groupName` 任务组，要确保和其他项目唯一 （必填）
- `registry` 注册中心，不配置则为单机模式 （选填）
- `configPersistHandler` 持久化配置支持，启动时合并配置和更新配置。（选填）
  
job参数说明：
- `jobName` 任务名，确保同一groupName下唯一
- `retries` 重试次数，默认不重试
- `cronExpr` 执行时间策略，如果配置了`configPersistHandler`以merge后的为准。
