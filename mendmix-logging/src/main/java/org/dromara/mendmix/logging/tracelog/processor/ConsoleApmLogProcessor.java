/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.processor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.logging.tracelog.TraceLogProcessor;
import org.dromara.mendmix.logging.tracelog.TraceSpan;

/**
 * <br>
 * @author vakinge
 * @date 2024年5月7日
 */
public class ConsoleApmLogProcessor implements TraceLogProcessor {

	@Override
	public void process(TraceSpan traceSpan) {
		StringBuilder builder = new StringBuilder(">>>>traceId:").append(traceSpan.getTraceId()).append("\n");
		process(traceSpan, builder, 1);
		System.out.println(builder.toString());
	}
	
	private void process(TraceSpan traceSpan,StringBuilder builder,int hierarchy) {
		List<TraceSpan> children = traceSpan.getChildren();
		if(children != null) {
			traceSpan.setChildren(null);
		}
		for (int i = 0; i < hierarchy; i++) {
			builder.append(StringUtils.SPACE);
		}
		builder.append("|-");
		builder.append(traceSpan.getTraceMethod()) //
		.append(",useTime:").append(traceSpan.getUseTime()) //
		.append(",traceKey:").append(traceSpan.getTraceKey()) //
		.append("\n");
		//
		if(children != null) {
			hierarchy++;
			for (TraceSpan child : children) {
				process(child, builder, hierarchy);
			}
		}
	}

}
