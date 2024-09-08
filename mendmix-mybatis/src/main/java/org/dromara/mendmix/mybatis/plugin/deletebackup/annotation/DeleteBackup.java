package org.dromara.mendmix.mybatis.plugin.deletebackup.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface DeleteBackup {

	String backupTo() default "";
	
	String updateTimeColumn() default "";
	
	String updateByColumn() default "";
}
