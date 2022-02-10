package com.jeesuite.springboot.autoconfigure;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jeesuite.springweb.SpringContextManager;

@Configuration  
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringContextManagerConfig extends SpringContextManager{

}
