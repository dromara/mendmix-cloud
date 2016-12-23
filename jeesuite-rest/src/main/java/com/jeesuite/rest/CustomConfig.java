/**
 * 
 */
package com.jeesuite.rest;

import com.jeesuite.rest.excetion.ExcetionWrapper;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月23日
 */
public interface CustomConfig {

	/**
	 * 对异常包装返回给前端
	 * @return
	 */
	ExcetionWrapper createExcetionWrapper();
	
	/**
	 * 返回要扫描的包（rest接口）
	 * @return
	 */
	String packages();
}
