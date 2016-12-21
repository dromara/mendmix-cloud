/**
 * 
 */
package com.jeesuite.test.sch;

import com.jeesuite.scheduler.ConfigPersistHandler;
import com.jeesuite.scheduler.model.JobConfig;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public class DbConfigPersistHandler implements ConfigPersistHandler {


	@Override
	public void merge(JobConfig config) {
		// load config from db
		System.out.println("========>>假装从数据库load一下配置");
	}

	@Override
	public void persist(JobConfig config) {
		// save config to db
		System.out.println("========>>假装保存配置到数据库");
	}

}
