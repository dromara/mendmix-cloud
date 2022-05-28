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
package com.mendmix.common.async;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * <br>
 * Class Name   : RetryTask
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年2月18日
 */
public abstract class RetryTask{
	
	private static final String STEP_POINT_KEY = "_stepPoint";
	
	public final long createTime = System.currentTimeMillis();
	private final Map<String, Object> contextParams = new HashMap<>(5);
	final ExecuteCallback callback;

	public RetryTask() {
		this.callback = null;
	}
	
	public RetryTask(ExecuteCallback callback) {
		this.callback = callback;
	}
	public abstract String traceId();
	public abstract boolean process() throws Exception;
	
	public void saveStepPoint(int point){
		contextParams.put(STEP_POINT_KEY, point);
	}
	
	public int getStepPoint(){
		return Integer.parseInt(contextParams.getOrDefault(STEP_POINT_KEY, 0).toString());
	}

}
