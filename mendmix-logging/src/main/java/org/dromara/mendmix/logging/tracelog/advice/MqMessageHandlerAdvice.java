/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.reflect.Method;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.counter.CirculateCounter;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.logging.tracelog.TraceAdviceDefine;
import org.dromara.mendmix.logging.tracelog.TraceSpan;
import org.dromara.mendmix.logging.tracelog.TraceType;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public class MqMessageHandlerAdvice extends BaseApmAdvice implements TraceAdviceDefine{
	
	 private static CirculateCounter samplingCounter = new CirculateCounter(10);
	 
	@Override
	public Junction<TypeDescription> typeMatcher() {
		return not(isInterface())
				.and(hasSuperType(named("org.dromara.mendmix.adapter.amqp.MessageHandler")));
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return named("process");
	}
	
    @Advice.OnMethodEnter
    public static void onMethodEnter(
    		@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.Origin Method method,
            @Advice.Argument(0) Object message) throws Throwable {
    	if(!CurrentRuntimeContext.isTraceLogging() && samplingCounter.next() > 0) {
    		return;
    	}
    	String traceArg = CachingFieldUtils.readField(message, "topic").toString();
    	span = startTrace(TraceType.mqConsumer,method, traceArg);
    }

    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span,
			@Advice.Thrown Throwable throwable) throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}

}
