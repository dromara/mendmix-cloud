/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.logging.tracelog.TraceAdviceDefine;
import org.dromara.mendmix.logging.tracelog.TraceSpan;
import org.dromara.mendmix.logging.tracelog.TraceType;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import okhttp3.Request;

/**
 * <br>
 * 
 * @author vakinge
 * @date 2024年5月6日
 */
public class OkHttp3Advice extends BaseApmAdvice implements TraceAdviceDefine {

	private static final String ORIGINAL_REQUEST_KEY = "originalRequest";

	@Override
	public Junction<TypeDescription> typeMatcher() {
		return ElementMatchers.named("okhttp3.internal.connection.RealCall");
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return ElementMatchers.named("execute").or(ElementMatchers.named("enqueue"));
	}

	@Advice.OnMethodEnter
	public static void onMethodEnter(@Advice.Local("traceSpan") TraceSpan span, @Advice.This Object target,
			@Advice.Origin Method method) throws Throwable {
		span = startTrace(TraceType.http, method, null);
		if (span != null) {
			Field field = CachingFieldUtils.getField(target.getClass(), ORIGINAL_REQUEST_KEY);
			Request request = (Request) field.get(target);
			span.setTraceKey(request.url().toString());
			if (request.header(CustomRequestHeaders.HEADER_TRACE_LOGGING) != null) {
				Request newRequest = request.newBuilder().headers(request.headers())
						.addHeader(CustomRequestHeaders.HEADER_REQUEST_ID, span.getTraceId())
						.addHeader(CustomRequestHeaders.HEADER_TRACE_LOGGING, String.valueOf(true)).build();
				field.set(target, newRequest);
			}
		}
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span, @Advice.Thrown Throwable throwable)
			throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}
}
