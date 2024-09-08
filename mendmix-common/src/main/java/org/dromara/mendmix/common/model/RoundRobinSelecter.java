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
package org.dromara.mendmix.common.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询选择器
 * 
 * <br>
 * Class Name   : RoundRobinSelecter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Nov 6, 2021
 */
public class RoundRobinSelecter {

	private int maxIndex;
	private AtomicInteger counter;
	
	public void incrNode() {
		this.maxIndex++;
	}
	
	public void decrNode() {
		this.maxIndex--;
	}

	public RoundRobinSelecter(int total) {
		this.maxIndex = total - 1;
		this.counter = new AtomicInteger(-1);
	}
	
	public int select() {
		if(maxIndex == 0)return 0;
		int next = counter.updateAndGet( (x) -> x >= maxIndex ? 0 : x + 1);
		return next;
	}
	
	public static void main(String[] args) {
		RoundRobinSelecter selecter = new RoundRobinSelecter(5);
		for (int i = 0; i < 100; i++) {
			System.out.println(selecter.select());
		}
	}
	
}
