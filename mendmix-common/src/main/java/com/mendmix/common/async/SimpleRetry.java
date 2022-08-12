/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.common.async;

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
					try {
						return errorCaller.call();
					} catch (Exception e2) {
						throw new RuntimeException(e2);
					}
				}else{					
					throw new RuntimeException(e);
				}
			}
		}
	}
}
