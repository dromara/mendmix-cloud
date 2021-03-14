/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.common.async;

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
