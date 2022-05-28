/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.test.sch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mendmix.scheduler.AbstractJob;
import com.mendmix.scheduler.JobContext;


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
