package com.jeesuite.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.jeesuite.gateway.error.ErrorHandlerController;
import com.jeesuite.gateway.zuul.router.CustomDiscoveryClientRouteLocator;  
  
@Configuration  
public class ZuulConfig {  
	
    @Bean  
    @ConditionalOnProperty(name = "eureka.client.enabled",havingValue = "true",matchIfMissing = true)
    public CustomDiscoveryClientRouteLocator discoveryClientRouteLocator(DiscoveryClient discovery,
			ZuulProperties properties, ServiceRouteMapper serviceRouteMapper,
			ServiceInstance localServiceInstance,ServerProperties server){
    	String servletPath = server.getServlet().getContextPath();
    	CustomDiscoveryClientRouteLocator discoveryRouteLocator = new CustomDiscoveryClientRouteLocator(servletPath, discovery, properties, serviceRouteMapper, localServiceInstance);
    	discoveryRouteLocator.setOrder(Ordered.LOWEST_PRECEDENCE);
    	return discoveryRouteLocator;
    }
    
    @Bean
    public ErrorHandlerController errorHandlerController() {
    	return new ErrorHandlerController();
    }
  
}  
