/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.reflect.Method;

import org.dromara.mendmix.logging.tracelog.TraceAdviceDefine;
import org.dromara.mendmix.logging.tracelog.TraceSpan;
import org.dromara.mendmix.logging.tracelog.TraceType;

import feign.Request;
import feign.Response;
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
public class FeignAdvice extends BaseApmAdvice implements TraceAdviceDefine{
	
	@Override
	public Junction<TypeDescription> typeMatcher() {
		return not(isInterface())
				.and(hasSuperType(named("feign.Client")));
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return nameStartsWith("execute")
				.and(takesArguments(2)).and(isPublic());
	}
	
    @Advice.OnMethodEnter
    public static void onMethodEnter(
    		@Advice.Local("traceSpan") TraceSpan span,
            @Advice.This Object target,
    		@Advice.Origin Method method,
            @Advice.Argument(0) Request request) throws Throwable {
    	String traceKey = request.url();
    	span = startTrace(TraceType.feign,method, traceKey);
    }

    
    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.Return Response response,
			@Advice.Thrown Throwable throwable) throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}

}
