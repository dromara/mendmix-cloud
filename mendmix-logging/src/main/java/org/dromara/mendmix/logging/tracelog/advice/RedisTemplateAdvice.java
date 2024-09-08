/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.advice;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.logging.tracelog.TraceAdviceDefine;
import org.dromara.mendmix.logging.tracelog.TraceSpan;
import org.dromara.mendmix.logging.tracelog.TraceType;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public class RedisTemplateAdvice extends BaseApmAdvice implements TraceAdviceDefine{

	@Override
	public Junction<TypeDescription> typeMatcher() {
		return hasSuperType(named("org.springframework.data.redis.core.AbstractOperations"))
				.or(named("org.springframework.data.redis.core.RedisTemplate"));
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return ElementMatchers.isPublic()
				.and(ElementMatchers.takesArgument(0, ElementMatchers.any()))
				.and(ElementMatchers.not(ElementMatchers.nameStartsWith("execute")));
	}
	
	@RuntimeType
    public Object intercept(@This Object target, @Origin Method targetMethod, @Argument(0) Object key, @SuperCall Callable<?> callable) throws Exception {
		TraceSpan span = startTrace(TraceType.redis,targetMethod, null);
		if(span == null) {
			return callable.call();
		}
		String traceKey;
		if(BeanUtils.isSimpleDataType(key)) {
			traceKey = key.toString();
		}else {
			traceKey = JsonUtils.toJson(key);
		}
		span.setTraceKey(traceKey);
		try {
			Object result = callable.call();
			endTrace(span);
	        return result;
		} catch (Exception e) {
			endTrace(span,e);
			throw e;
		}
    }

}
