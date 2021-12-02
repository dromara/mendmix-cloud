package com.jeesuite.amqp;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
/**
 * 
 * <br>
 * Class Name   : MQTopicRef
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public @interface MQTopicRef {
    String value();
}
