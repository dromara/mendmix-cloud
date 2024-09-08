/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.logging.LogConfigs;
import org.dromara.mendmix.logging.tracelog.advice.BaseApmAdvice;
import org.dromara.mendmix.logging.tracelog.processor.ConsoleApmLogProcessor;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public class ChainTraceContext {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.logging");
	
	private static final String CONTEXT_TRACE_NAME = "__ctx_chain_trace_name~";
	
	private static TraceLogProcessor logProcessor;
	
	private TraceSpan mainSpan;
	private TraceSpan activeSpan;
	
	private static ChainTraceContext get() {
		if(!LogConfigs.TRACE_LOGGING || GlobalContext.isStarting()) {
			return null;
		}
		return ThreadLocalContext.get(CONTEXT_TRACE_NAME);
	}
	
	public static void setLogProcessor(TraceLogProcessor logProcessor) {
		ChainTraceContext.logProcessor = logProcessor;
	}

	public static TraceLogProcessor getLogProcessor() {
		if(logProcessor != null)return logProcessor;
		if(GlobalContext.isStarting())return null;
		synchronized (ChainTraceContext.class) {
			if(logProcessor != null)return logProcessor;
			logProcessor = InstanceFactory.getInstance(TraceLogProcessor.class);
			if(logProcessor == null) {
				logProcessor = new ConsoleApmLogProcessor();
			}
		}
		return logProcessor;
	}



	public static TraceSpan start(TraceType traceType,String method,String traceKey) {
		if(!LogConfigs.TRACE_LOGGING || !CurrentRuntimeContext.isTraceLogging() || getLogProcessor() == null)return null;
		ChainTraceContext context = new ChainTraceContext();
		TraceSpan span = TraceSpan.buildTraceSpan(traceType, method, traceKey);
		context.mainSpan = span;
		context.activeSpan = span;
		ThreadLocalContext.set(CONTEXT_TRACE_NAME, context);
		return span;
	}
	
	
    public static void end(Throwable throwable) {
    	if(!LogConfigs.TRACE_LOGGING)return;
    	ChainTraceContext context = ThreadLocalContext.get(CONTEXT_TRACE_NAME);
    	if(context == null)return;
    	try {			
    		BaseApmAdvice.endTrace(context.mainSpan, throwable);
    		logProcessor.process(context.mainSpan);
		} catch (Exception e) {
			logger.debug(">>>>>process traceLog error:{}",e.getMessage());
		}
    }

	public static TraceSpan mainSpan() {
		ChainTraceContext context = get();
		if(context == null)return null;
		return context.mainSpan;
	}

	public static TraceSpan activeSpan() {
		ChainTraceContext context = get();
		if(context == null)return null;
		return context.activeSpan;
	}

	public static ChainTraceContext activeSpan(TraceSpan activeSpan) {
		ChainTraceContext context = get();
		if(context == null)return null;
		context.activeSpan = activeSpan;
		return context;
	}
    
}
