/**
 * 
 */
package com.jeesuite.test.sch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jeesuite.scheduler.AbstractJob;
import com.jeesuite.scheduler.JobContext;


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
			System.out.println("<<<<<<<DemoParallelTask=====>"+count);
		}
		count++;
	}
	
	@Override
	public boolean parallelEnabled() {
		//开启并行计算
		return true;
	}

}
