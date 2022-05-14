package com.jeesuite.common.http;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月23日
 */
public interface ProxyResolver {

	String resolve(String origin);
}
