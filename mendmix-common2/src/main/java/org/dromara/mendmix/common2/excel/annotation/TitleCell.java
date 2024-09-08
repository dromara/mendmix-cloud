/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.common.excel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TitleCell {

    /**
     * 在excel文件中某列数据的名称
     *
     * @return 名称
     */
    String name();

    /**
     * 列序列 
     *
     * @return 顺序
     */
    int column();
    
    int row() default 1;
    
    boolean notNull() default false;
    
    String format() default "";
    
    int width() default 15;
    
    /**
     * 嵌套父级列名称
     */
    String parentName() default "";
    
    Class<?> type() default String.class;
}
