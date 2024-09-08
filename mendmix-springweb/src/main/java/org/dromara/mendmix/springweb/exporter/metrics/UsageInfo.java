/**
 * 
 */
package org.dromara.mendmix.springweb.exporter.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2024年7月12日
 */
public class UsageInfo {

	private static final BigDecimal _100 = new BigDecimal(100);
	
	public long min;
	public long max;
	public long used;
	
	public UsageInfo() {}

	public UsageInfo(long min, long max, long used) {
		this.min = min;
		this.max = max;
		this.used = used;
	}
	
	public double usagePercent() {
		if(max == 0)return 0;
		return new BigDecimal(used)
				.divide(new BigDecimal(max), 3, RoundingMode.HALF_UP)
				.multiply(_100)
				.doubleValue();
	}
	
}
