/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.common.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.dromara.mendmix.common.async.AsyncInitializer;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.util.SpringAopHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 
 * 
 * <br>
 * Class Name   : GlobalInternalScheduleService
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Jan 8, 2022
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalInternalScheduleService implements InitializingBean, DisposableBean,ApplicationContextAware ,AsyncInitializer {

	private static Logger log = LoggerFactory.getLogger("global.internal.task");
	
	private ApplicationContext applicationContext;

	private List<SubTimerTaskStat> taskStats = new ArrayList<>();
	private ScheduledExecutorService executor;
	
	public GlobalInternalScheduleService() {}
	
	public GlobalInternalScheduleService(List<SubTimerTask> tasks) {
		for (SubTimerTask task : tasks) {
			taskStats.add(new SubTimerTaskStat(task));
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}


	@Override
	public void destroy() throws Exception {
		if (executor == null)
			return;
		executor.shutdown();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Map<String, SubTimerTask> taskMap = applicationContext.getBeansOfType(SubTimerTask.class);
		if (taskMap != null) {
			for (SubTimerTask task : taskMap.values()) {
				taskStats.add(new SubTimerTaskStat(task));
			}
		}
		
		if(taskStats.isEmpty())return;

		executor = Executors.newScheduledThreadPool(1, new StandardThreadFactory("globalInternalScheduler"));
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				for (SubTimerTaskStat taskStat : taskStats) {
					try {
						Thread.sleep(10);
					} catch (Exception e) {
					}
					execSubTimerTask(taskStat);
				}
			}
		}, 1000, 5000, TimeUnit.MILLISECONDS);

		log.info("MENDMIX-TRACE-LOGGGING-->> init GlobalInternalScheduleService finish -> subtaskNums:{}", taskStats.size());
	}

	protected void execSubTimerTask(SubTimerTaskStat taskStat) {
		if (taskStat.running)
			return;
		long currentTime = System.currentTimeMillis();
		if (currentTime - taskStat.lastFireTime < taskStat.task.interval()) {
			return;
		}
		taskStat.running = true;
		try {
			if (log.isDebugEnabled())
				log.debug("MENDMIX-TRACE-LOGGGING-->> InternalSchedule[{}] execute Begin..", taskStat.taskName);
			taskStat.task.doSchedule();
			taskStat.lastFireTime = currentTime;
		} catch (Exception e) {
			log.error("MENDMIX-TRACE-LOGGGING-->> InternalSchedule["+taskStat.taskName+"] execute",e);
		} finally {
			taskStat.running = false;
		}
	}

	private class SubTimerTaskStat {
		SubTimerTask task;
		String taskName;
		boolean running = false;
		long lastFireTime;

		public SubTimerTaskStat(SubTimerTask task) {
			this.task = task;
			try {
				this.taskName = SpringAopHelper.getTarget(task).getClass().getSimpleName();
			} catch (Exception e) {
				this.taskName = task.getClass().getSimpleName();
			}
			if(task.delay() > 0) {
				this.lastFireTime = System.currentTimeMillis() + task.delay();
			}else {
				//确保启动执行
				this.lastFireTime = System.currentTimeMillis() - 3600 * 24 * 1000;
			}
		}
	}


	@Override
	public void doInitialize() {
		for (SubTimerTaskStat taskStat : taskStats) {
			if(taskStat.task.delay() > 0)continue;
			execSubTimerTask(taskStat);
		}
	}

}
