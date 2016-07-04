/**
 * 
 */
package com.jeesuite.cache;

import java.util.Date;

import com.jeesuite.common.util.DateUtils;

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
