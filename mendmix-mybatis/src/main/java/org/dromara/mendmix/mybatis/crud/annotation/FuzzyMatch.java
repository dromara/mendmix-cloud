package org.dromara.mendmix.mybatis.crud.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月15日
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface FuzzyMatch {

}
