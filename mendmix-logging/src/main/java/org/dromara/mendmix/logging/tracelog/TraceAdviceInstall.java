/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.dromara.mendmix.logging.tracelog.advice.ApacheHttpClientAdvice;
import org.dromara.mendmix.logging.tracelog.advice.FeignAdvice;
import org.dromara.mendmix.logging.tracelog.advice.JDBCConnectionAdvice;
import org.dromara.mendmix.logging.tracelog.advice.JDBCDataSourceAdvice;
import org.dromara.mendmix.logging.tracelog.advice.JDBCStatementAdvice;
import org.dromara.mendmix.logging.tracelog.advice.OkHttp3Advice;
import org.dromara.mendmix.logging.tracelog.advice.RedisConnectionFactoryAdvice;
import org.dromara.mendmix.logging.tracelog.advice.RedisTemplateAdvice;
import org.springframework.util.ClassUtils;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public class TraceAdviceInstall {

	private static boolean installed = false;
	
	public static void install(boolean isGateway) {
		if(installed)return;
		installed = true;
		//已经安装过？？
		ByteBuddyAgent.install();
		final DefaultAgentListener listener = new DefaultAgentListener();
		if(!isGateway) {			
			installComponent(new JDBCDataSourceAdvice(), listener);
			installComponent(new JDBCConnectionAdvice(), listener);
			installComponent(new JDBCStatementAdvice(), listener);
			installComponent(new FeignAdvice(), listener);
		}
		installComponent(new RedisConnectionFactoryAdvice(), listener);
		installComponent(new RedisTemplateAdvice(), listener);
		if(ClassUtils.isPresent("org.apache.http.client.methods.HttpRequestWrapper", null)) {			
			installComponent(new ApacheHttpClientAdvice(), listener);
		}
		if(ClassUtils.isPresent("okhttp3.OkHttpClient", null)) {			
			installComponent(new OkHttp3Advice(), listener);
		}

	}
	
	private static void installComponent(TraceAdviceDefine adviceDefine,DefaultAgentListener listener) {
        AgentBuilder agentBuilder = new AgentBuilder.Default().with(listener);
		
        boolean withRuntimeType = MethodUtils.getMethodsWithAnnotation(adviceDefine.getClass(), RuntimeType.class).length > 0;
        if(withRuntimeType) {
        	agentBuilder.ignore(ElementMatchers.none())
            .type(adviceDefine.typeMatcher())
            .transform((builder, typeDescription, classLoader, module) -> {
            	return builder.method(adviceDefine.methodMatcher())
                              .intercept(MethodDelegation.to(adviceDefine));
                }).installOnByteBuddyAgent();
        }else {
        	AgentBuilder.Transformer.ForAdvice transformer = new AgentBuilder.Transformer.ForAdvice()
    				.advice(adviceDefine.methodMatcher(), adviceDefine.getClass().getName());
            agentBuilder.ignore(ElementMatchers.none())
                    .type(adviceDefine.typeMatcher())
                    .transform(transformer)
                    .installOnByteBuddyAgent();
        }
	}
	
	static class DefaultAgentListener implements AgentBuilder.Listener {

        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule javaModule, boolean b) {
        }

        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                boolean loaded, DynamicType dynamicType) {
        }

        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                boolean loaded) {
        }

        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                Throwable throwable) {
        	System.err.println("Error transforming class:" + typeName);
            throwable.printStackTrace(System.err);
        }

        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        }
    }
}
