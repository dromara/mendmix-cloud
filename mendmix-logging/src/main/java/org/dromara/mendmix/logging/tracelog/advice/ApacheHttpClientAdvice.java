/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.reflect.Method;

import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.conn.routing.HttpRoute;
import org.dromara.mendmix.common.CustomRequestHeaders;
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
public class ApacheHttpClientAdvice  extends BaseApmAdvice implements TraceAdviceDefine{
	
	@Override
	public Junction<TypeDescription> typeMatcher() {
		return named("org.apache.http.impl.execchain.ProtocolExec");
		//return hasSuperType(named("org.apache.http.impl.execchain.ClientExecChain"));
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return named("execute").and(takesArguments(4))
				.and(returns(hasSuperType(named("org.apache.http.client.methods.CloseableHttpResponse"))))
				.and(takesArgument(0, hasSuperType(named("org.apache.http.conn.routing.HttpRoute"))))
				.and(takesArgument(1, hasSuperType(named("org.apache.http.client.methods.HttpRequestWrapper"))))
				.and(takesArgument(2, hasSuperType(named("org.apache.http.client.protocol.HttpClientContext"))))
				.and(takesArgument(3, hasSuperType(named("org.apache.http.client.methods.HttpExecutionAware"))));
	}
	
    @Advice.OnMethodEnter
    public static void onMethodEnter(
    		@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.Origin Method method,
    		@Advice.Argument(0) HttpRoute route, 
    		@Advice.Argument(1) HttpRequestWrapper request) throws Throwable {
    	String traceKey = request.getURI().toString();
    	span = startTrace(TraceType.http,method, traceKey);
    	if(span != null && !request.containsHeader(CustomRequestHeaders.HEADER_TRACE_LOGGING)) {
    		request.addHeader(CustomRequestHeaders.HEADER_REQUEST_ID, span.getTraceId());
    		request.addHeader(CustomRequestHeaders.HEADER_TRACE_LOGGING, String.valueOf(true));
    	}
    }
    
    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.Return Object returnValue,
			@Advice.Thrown Throwable throwable) throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}

}
