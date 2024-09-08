/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import java.lang.reflect.Method;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.logging.tracelog.ChainTraceContext;
import org.dromara.mendmix.logging.tracelog.TraceSpan;
import org.dromara.mendmix.logging.tracelog.TraceType;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public abstract class BaseApmAdvice {
	
	public static TraceSpan startTrace(TraceType traceType,Method method,String traceArg) {
		TraceSpan activeSpan = ChainTraceContext.activeSpan();
		if(activeSpan == null)return null;
		//忽略重载方法
		String traceMethodName = activeSpan.getTraceType() + activeSpan.getTraceMethod().substring(activeSpan.getTraceMethod().lastIndexOf(GlobalConstants.DOT) + 1);
		if(traceMethodName.equals(traceType.name() + method.getName())) {
			return null;
		}
		String fullName = new StringBuilder(method.getDeclaringClass().getName()).append(GlobalConstants.DOT).append(method.getName()).toString();
		TraceSpan traceSpan = TraceSpan.buildTraceSpan(traceType, fullName, traceArg);
		activeSpan.addChild(traceSpan);
		ChainTraceContext.activeSpan(traceSpan);
		return traceSpan;
	}
	
	public static TraceSpan startTrace(TraceType traceType,String methodName,String traceArg) {
		TraceSpan activeSpan = ChainTraceContext.activeSpan();
		if(activeSpan == null)return null;
		TraceSpan traceSpan = TraceSpan.buildTraceSpan(traceType, methodName, traceArg);
		activeSpan.addChild(traceSpan);
		ChainTraceContext.activeSpan(traceSpan);
		return traceSpan;
	}
	
	public static void endTrace(TraceSpan span) {
		span.end();
		if(span.getEndTime() != null && span.getParent() != null) {
			ChainTraceContext.activeSpan(span.getParent());
		}
	}
	
	public static void endTrace(TraceSpan span,Throwable throwable) {
		if(span == null)return;
		String exception = throwable == null ? null : ExceptionFormatUtils.buildExceptionMessages(throwable, 3);
		endTrace(span, exception);
	}
	
	public static void endTrace(TraceSpan span,String exception) {
		if(span == null)return;
        if(exception != null) {
        	span.endWithException(exception);
		}else {			
			span.end();
		}
		if(span.getEndTime() != null && span.getParent() != null) {
			ChainTraceContext.activeSpan(span.getParent());
		}
	}
}
