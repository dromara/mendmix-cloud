package com.jeesuite.common2.excel.annotation;

import java.lang.annotation.*;


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
