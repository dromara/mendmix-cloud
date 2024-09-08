/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
public interface TraceAdviceDefine {
	
	Junction<TypeDescription> typeMatcher();
	ElementMatcher<? super MethodDescription> methodMatcher();
}
