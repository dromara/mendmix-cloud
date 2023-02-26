package com.mendmix.mybatis.plugin.rewrite.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TablePermissionStrategy {

	String table();
	String[] columns() default {};
	boolean handleOwner() default true;
	String[] orRelations() default {}; //["a,b","c,d,e"]
	String[] ownerColumns() default {};
}
