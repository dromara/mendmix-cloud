/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE, METHOD })
/**
 * <br>
 * @author vakinge
 * @date 2024年2月29日
 */
public @interface ChainTraceIgnore {

}
