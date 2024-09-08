/**
 * 
 */
package org.dromara.mendmix.mybatis.plugin.operProtect.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2024年5月29日
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Inherited
@Documented
public @interface SensitiveOperProtect {
    boolean value() default true;
}
