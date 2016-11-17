/**
 * 
 */
package com.jeesuite.common2.lock;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年7月22日
 */
public interface LockCaller<T> {

	/**
	 * 持有锁的操作
	 * @return
	 */
	T onHolder();
	
	/**
	 * 等待锁的操作
	 * @return
	 */
	T onWait();
}
