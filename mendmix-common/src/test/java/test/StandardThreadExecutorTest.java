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
package test;

import org.apache.commons.lang3.RandomUtils;

import com.mendmix.common.async.StandardThreadExecutor;

/**
 * 
 * <br>
 * Class Name   : StandardThreadExecutorTest
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Mar 8, 2022
 */
public class StandardThreadExecutorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StandardThreadExecutor executor = new StandardThreadExecutor(1, 5, 50);
		while(true) {
			executor.submit(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("thread-" + Thread.currentThread().getName());
					try {Thread.sleep(RandomUtils.nextLong(5000, 30000));} catch (Exception e) {}
				}
			});
		}

	}

}
