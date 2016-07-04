/**
 * 
 */
package com.jeesuite.test.sch;

import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.scheduler.BaseScheduler;

public class DemoTask extends BaseScheduler{

	int count = 1;
	@Override
	public void doJob() throws Exception {
		System.out.println("DemoTask1=====>"+count);
		Thread.sleep(RandomUtils.nextLong(1000, 2000));
		count++;
	}

}
