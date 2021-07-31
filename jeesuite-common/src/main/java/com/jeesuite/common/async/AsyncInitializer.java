package com.jeesuite.common.async;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步初始化接口
 * <br>
 * Class Name   : AsyncInitializer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年8月27日
 */
public interface AsyncInitializer {

	static AtomicInteger count = new AtomicInteger(1);
	
	default void process(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncInitialize();
			}
		}, "CacheInitializer-"+ count.getAndIncrement()).start();
	}
	
	void asyncInitialize();
}
