/**
 * 
 */
package com.jeesuite.test.sch;

import com.jeesuite.scheduler.BaseScheduler;

public class DemoTask2 extends BaseScheduler{

	int count = 1;
	@Override
	public void doJob() throws Exception {
		System.out.println("DemoTask2=====>"+count);
		count++;
	}

}
