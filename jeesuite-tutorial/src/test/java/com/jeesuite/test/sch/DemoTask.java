/**
 * 
 */
package com.jeesuite.test.sch;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.scheduler.AbstractJob;
import com.jeesuite.scheduler.JobContext;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年1月28日
 * @Copyright (c) 2015, lifesense.com
 */
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
		return false;
	}

}
