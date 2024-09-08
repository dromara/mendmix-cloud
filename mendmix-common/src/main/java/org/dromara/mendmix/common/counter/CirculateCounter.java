/**
 * 
 */
package org.dromara.mendmix.common.counter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <br>
 * @author vakinge
 * @date 2024年3月1日
 */
public class CirculateCounter {

	private int maxCount;
	private AtomicInteger counter = new AtomicInteger(0);

	public CirculateCounter(int maxCount) {
		if(maxCount <= 0) {
			throw new IllegalArgumentException("maxCount must > 0");
		}
		this.maxCount = maxCount;
	}
	
	public int next() {
		if(maxCount == 1)return 0;
		return counter.updateAndGet( (x) -> x >= maxCount ? 0 : x + 1);
	}
}
