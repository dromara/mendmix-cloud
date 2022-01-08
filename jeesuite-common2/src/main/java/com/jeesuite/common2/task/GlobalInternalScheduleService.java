package com.jeesuite.common2.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.jeesuite.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.spring.ApplicationStartedListener;
import com.jeesuite.spring.InstanceFactory;

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
public class GlobalInternalScheduleService implements InitializingBean, DisposableBean ,ApplicationStartedListener{

	private static Logger log = LoggerFactory.getLogger("global.internal.task");

	private List<SubTimerTaskStat> taskStats = new ArrayList<>();
	private ScheduledExecutorService executor;

	@Override
	public void destroy() throws Exception {
		if (executor == null)
			return;
		executor.shutdown();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Map<String, SubTimerTask> taskMap = InstanceFactory.getInstanceProvider().getInterfaces(SubTimerTask.class);
		if (taskMap == null || taskMap.isEmpty())
			return;
		//
		for (SubTimerTask task : taskMap.values()) {
			taskStats.add(new SubTimerTaskStat(task));
		}

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

		log.info("init GlobalInternalScheduleService finish -> subtaskNums:{}", taskStats.size());
	}

	protected void execSubTimerTask(SubTimerTaskStat taskStat) {
		if (taskStat.running)
			return;
		long currentTime = System.currentTimeMillis();
		if (currentTime - taskStat.lastFireTime < taskStat.task.periodMillis()) {
			return;
		}
		taskStat.running = true;
		try {
			if (log.isDebugEnabled())
				log.debug("SubTimerTask[{}] execute Begin..", taskStat.task.getClass().getName());
			taskStat.task.doSchedule();
			taskStat.lastFireTime = currentTime;
		} catch (Exception e) {
			log.error("SubTimerTask[{}] execute Error:{}", taskStat.task.getClass().getName(),
					ExceptionUtils.getMessage(e));
		} finally {
			taskStat.running = false;
		}
	}

	private class SubTimerTaskStat {
		SubTimerTask task;
		boolean running = false;
		long lastFireTime;

		public SubTimerTaskStat(SubTimerTask task) {
			this.task = task;
			// 确保启动执行
			this.lastFireTime = System.currentTimeMillis() - 3600 * 24 * 1000;
		}
	}


	@Override
	public void onApplicationStarted(ApplicationContext applicationContext) {
		for (SubTimerTaskStat taskStat : taskStats) {
			execSubTimerTask(taskStat);
		}
	}

}
