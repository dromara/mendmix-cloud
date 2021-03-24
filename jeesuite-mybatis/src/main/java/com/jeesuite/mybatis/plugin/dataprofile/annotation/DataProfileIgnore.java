package com.jeesuite.mybatis.plugin.dataprofile.annotation;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

@Documented
@Target(METHOD)
public @interface DataProfileIgnore {

}
