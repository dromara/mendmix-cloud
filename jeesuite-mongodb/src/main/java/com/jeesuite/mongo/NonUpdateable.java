/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mongo;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 标记字段忽略更新
 * <br>
 * Class Name   : NonUpdateable
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年7月23日
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface NonUpdateable {

}
