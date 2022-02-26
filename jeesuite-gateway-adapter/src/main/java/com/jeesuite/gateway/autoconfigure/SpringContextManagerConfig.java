package com.jeesuite.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jeesuite.springweb.SpringContextManager;

@Configuration("zuulSpringContextManagerConfig")  
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnMissingBean(SpringContextManager.class)
public class SpringContextManagerConfig extends SpringContextManager{

}
