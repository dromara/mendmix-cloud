/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;

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
 * @date 2024年3月1日
 */
public class HttpUrlConnectionAdvice  extends BaseApmAdvice implements TraceAdviceDefine{
	
	@Override
	public Junction<TypeDescription> typeMatcher() {
		//javax.net.ssl.HttpsURLConnection
		return is(HttpURLConnection.class);
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return named("connect").and(takesArguments(0)).or(named("getOutputStream").and(takesArguments(0)))
				.or(named("getInputStream").and(takesArguments(0)));
	}
	
    @Advice.OnMethodEnter
    public static void onMethodEnter(
    		@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.This HttpURLConnection thiz,
    		@Advice.Origin Method method) throws Throwable {
    	String traceArg = thiz.getURL().toString();
    	span = startTrace(TraceType.http,method, traceArg);
    }

    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span,
			@Advice.Thrown Throwable throwable) throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}

}
