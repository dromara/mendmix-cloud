package com.jeesuite.common.model;

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
