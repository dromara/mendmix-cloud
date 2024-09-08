/**
 * 
 */
package org.dromara.mendmix.mybatis.datasource;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Inherited
@Documented
public @interface UsingDataSource {

	String group() default "default";
	
	String[] dataSourceKey() default {};
}
