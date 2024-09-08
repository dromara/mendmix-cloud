/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.cache;

import java.util.Date;

import org.dromara.mendmix.common.util.DateUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年4月28日
 */
public class CacheExpires {
	
	public final static long IN_1MIN = 60;
	
	public final static long IN_3MINS = 60 * 3; 
	
	public final static long IN_5MINS = 60 * 5;

	public final static long IN_1HOUR = 60 * 60;
	
	public final static long IN_HALF_HOUR = 60 * 30;
	
	public final static long IN_1DAY = IN_1HOUR * 24;
	
	public final static long IN_1WEEK = IN_1DAY * 7;
	
	public final static long IN_1MONTH = IN_1DAY * 30;
	
	/**
	 * 当前时间到今天结束相隔的秒
	 * @return
	 */
	public static long todayEndSeconds(){
		Date curTime = new Date();
		return DateUtils.getDiffSeconds(DateUtils.getDayEnd(curTime), curTime);
	}
	
}
