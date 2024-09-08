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
package org.dromara.mendmix.common.util;

import java.io.Serializable;

import org.dromara.mendmix.common.util.ExceptionFormatUtils;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2021年9月30日
 */
public class LogMessageFormat {

	/**
	 * 生成日志消息
	 * @param actionKey 操作关键词
	 * @param bizKey 业务关键信息如：订单号
	 * @return
	 */
	public static String buildLogHeader(String actionKey,Serializable bizKey) {
		return ExceptionFormatUtils.buildLogHeader(actionKey, bizKey);
	}
	
	
	public static String buildLogTail() {
		return ExceptionFormatUtils.buildLogTail();
	}
	
	public static String buildExceptionMessages(Throwable throwable) {
		return ExceptionFormatUtils.buildExceptionMessages(throwable);
	}
	
	public static String buildExceptionMessages(Throwable throwable,int showLines) {
    	return ExceptionFormatUtils.buildExceptionMessages(throwable, showLines);
	}
	
	public static String buildExceptionMessages(Throwable throwable,boolean filterMode) {
		return ExceptionFormatUtils.buildExceptionMessages(throwable, filterMode);
	}
}
