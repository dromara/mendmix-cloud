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
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.reflect.Method;

import org.dromara.mendmix.logging.tracelog.TraceAdviceDefine;
import org.dromara.mendmix.logging.tracelog.TraceSpan;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月29日
 */
public class JDBCCallableStatementAdvice  extends BaseApmAdvice implements TraceAdviceDefine{
	
	@Override
	public Junction<TypeDescription> typeMatcher() {
		return not(isInterface())
				.and(hasSuperType(named("java.sql.Statement")))
				.and(nameStartsWith("com.mysql."));
	}

	@Override
	public ElementMatcher<? super MethodDescription> methodMatcher() {
		return nameStartsWith("set")
				.and(takesArgument(0, String.class).or(takesArgument(0, int.class)))
				.and(takesArguments(2)).and(isPublic());
	}
	
    @Advice.OnMethodEnter
    public static void onMethodEnter(
    		@Advice.Local("traceSpan") TraceSpan span,
    		@Advice.This Object thiz,
    		@Advice.Origin Method method,
    		@Advice.Argument(0) Object parameterName, //
			@Advice.Argument(1) Object parameterValue) throws Throwable {
    	//span = startTrace(TraceType.jdbc,method, null);
    	System.out.println(String.format(" >>parameterName:%s,parameterValue:%s", parameterName,parameterValue));
    }

    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void onMethodExit(@Advice.Local("traceSpan") TraceSpan span,
			@Advice.Thrown Throwable throwable) throws Throwable {
		if (span != null) {
			endTrace(span, throwable);
		}
	}

}

