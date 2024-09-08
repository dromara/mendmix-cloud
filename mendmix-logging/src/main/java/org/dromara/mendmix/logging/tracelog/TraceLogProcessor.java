/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

/**
 * <br>
 * @author vakinge
 * @date 2024年5月7日
 */
public interface TraceLogProcessor {

	void process(TraceSpan traceSpan);
}
