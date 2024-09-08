/**
 * 
 */
package org.dromara.mendmix.logging.applog;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.logging.LogConstants;
import org.slf4j.MDC;

/**
 * <br>
 * @author vakinge
 * @date 2024年6月21日
 */
public class LogContextHelper {

	public static void initContext() {
		//ThreadContext.put("traceId", CurrentRuntimeContext.getRequestId());
		MDC.put(LogConstants.TRACE_ID, CurrentRuntimeContext.getRequestId());
	}
	
	public static void unsetContext() {
		MDC.clear();
	}
}
