package com.jeesuite.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public abstract class AbstractRequestFilter implements GlobalFilter, Ordered {

    static Logger logger = LoggerFactory.getLogger("com.jeesuite.gateway");
    
    private GatewayFilter delegate;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -999;
    }
}