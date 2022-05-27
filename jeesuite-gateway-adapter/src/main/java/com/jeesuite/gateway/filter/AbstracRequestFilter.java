package com.jeesuite.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.WrapperResponse;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.gateway.GatewayConfigs;
import com.jeesuite.gateway.GatewayConstants;
import com.jeesuite.gateway.filter.pre.GlobalHeaderHanlder;
import com.jeesuite.gateway.filter.pre.RequestLogHanlder;
import com.jeesuite.gateway.filter.pre.SignatureRequestHandler;
import com.jeesuite.gateway.helper.RuequestHelper;
import com.jeesuite.gateway.model.BizSystemModule;

import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public abstract class AbstracRequestFilter implements GlobalFilter, Ordered {

	private static Logger logger = LoggerFactory.getLogger("com.jeesuite.gateway");
    
    private List<String> ignoreUris = Arrays.asList("/actuator/health");
    
    private List<PreFilterHandler> handlers = new ArrayList<>();
    
    public AbstracRequestFilter(PreFilterHandler...filterHandlers) {
		handlers.add(new GlobalHeaderHanlder());
		
		if(GatewayConfigs.actionLogEnabled) {
			handlers.add(new RequestLogHanlder());
		}
		
		if(GatewayConfigs.openEnabled) {
			handlers.add(new SignatureRequestHandler());
		}

		boolean has = filterHandlers != null && filterHandlers.length > 0 && filterHandlers[0] != null;
		if(has) {
			for (PreFilterHandler filterHandler : filterHandlers) {
				handlers.add(filterHandler);
			}
		}
		if(handlers.size() > 1) {			
			handlers.stream().sorted(Comparator.comparing(PreFilterHandler::order));
		}

	}
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    	
    	exchange.getAttributes().put(GatewayConstants.CONTEXT_REQUEST_START_TIME, System.currentTimeMillis());

    	String requestUri = exchange.getRequest().getPath().value();
    	if(ignoreUris.stream().anyMatch(o -> requestUri.endsWith(o))) {
    		exchange.getAttributes().put(GatewayConstants.CONTEXT_IGNORE_FILTER, Boolean.TRUE);
    		return chain.filter(exchange);
    	}
    	
    	BizSystemModule module = RuequestHelper.getCurrentModule(exchange);
    	try {
    		Builder requestBuilder = exchange.getRequest().mutate();
    		for (PreFilterHandler handler : handlers) {
    			requestBuilder = handler.process(exchange, module,requestBuilder);
    		}
    		exchange = exchange.mutate().request(requestBuilder.build()).build();
		} catch (Exception e) {
			exchange.getAttributes().clear();
			if(e instanceof JeesuiteBaseException == false) {
				logger.error("requestFilter_error",e);
			}
			ServerHttpResponse response = exchange.getResponse();
			byte[] bytes = JsonUtils.toJson(WrapperResponse.fail(e)).getBytes(StandardCharsets.UTF_8);
			return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
		}
    	
    	
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
    	//after AdaptCachedBodyGlobalFilter
        return Ordered.HIGHEST_PRECEDENCE + 1001;
    }
    
    
}