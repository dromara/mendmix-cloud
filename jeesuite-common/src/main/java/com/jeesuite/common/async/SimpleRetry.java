package com.jeesuite.common.async;

import org.apache.commons.lang3.RandomUtils;

/**
 * 
 * <br>
 * Class Name   : SimpleRetry
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年3月15日
 */
public class SimpleRetry<T> {

	private ICaller<T> caller;
	private ICaller<T> errorCaller;
	
	public SimpleRetry(ICaller<T> caller) {
		super();
		this.caller = caller;
	}
	
	public SimpleRetry<T> onError(ICaller<T> errorCaller){
		this.errorCaller = errorCaller;
		return this;
	}

	public T doRetry(int retries){
		try {
			return caller.call();
		} catch (Exception e) {
			if(--retries > 0){
				try {Thread.sleep(RandomUtils.nextLong(10, 500));} catch (Exception e2) {}
				return doRetry(retries);
			}else{
				if(this.errorCaller != null){
					return errorCaller.call();
				}else{					
					throw new RuntimeException(e);
				}
			}
		}
	}
}
