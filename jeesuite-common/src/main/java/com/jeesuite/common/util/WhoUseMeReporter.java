/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 这个一个上报工具：仅仅是为了知道谁在用我们
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @website <a href="http://www.jeesuite.com">vakin</a>
 * @date 2019年6月18日
 */
public class WhoUseMeReporter {
	
	public static void report() {
		Map<String, String> params = new HashMap<>();
		String packageName = ResourceUtils.getProperty("jeesuite.application.base-package");
		if(packageName == null) {
			List<String> list = ResourceUtils.getList("mybatis.type-aliases-package");
			packageName = list.isEmpty() ? "" : list.get(0);
		}
		params.put("packageName", packageName);
		
		final String json = JsonUtils.toJson(params);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {HttpUtils.postJson("http://www.jeesuite.com/active/report", json);} catch (Exception e) {}
			}
		}).start();
	}
}
