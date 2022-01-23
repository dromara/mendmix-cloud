package com.jeesuite.zuul.autoconfigure;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.jeesuite.springweb.SpringContextManager;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringContextManagerConfig extends SpringContextManager{

}
