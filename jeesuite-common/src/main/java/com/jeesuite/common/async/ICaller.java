package com.jeesuite.common.async;

/**
 * 
 * <br>
 * Class Name   : ICaller
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年7月28日
 */
public interface ICaller<V> {
   
	V call(Object...args);
}
