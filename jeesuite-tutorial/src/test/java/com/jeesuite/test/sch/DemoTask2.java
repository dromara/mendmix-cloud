/**
 * 
 */
package com.jeesuite.test.sch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jeesuite.scheduler.AbstractJob;
import com.jeesuite.scheduler.JobContext;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年1月28日
 * @Copyright (c) 2015, lifesense.com
 */
public class DemoTask2 extends AbstractJob{

	int count = 1;
	@Override
	public void doJob(JobContext context) throws Exception {
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
		return true;
	}

}
