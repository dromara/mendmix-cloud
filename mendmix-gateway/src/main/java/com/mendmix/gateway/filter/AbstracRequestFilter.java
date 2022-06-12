/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.gateway.filter;

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

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.async.AsyncInitializer;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.pre.GlobalHeaderHanlder;
import com.mendmix.gateway.filter.pre.RequestLogHanlder;
import com.mendmix.gateway.filter.pre.SignatureRequestHandler;
import com.mendmix.gateway.helper.RuequestHelper;
import com.mendmix.gateway.model.BizSystemModule;

import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public abstract class AbstracRequestFilter implements GlobalFilter, Ordered,AsyncInitializer {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");
    
    private List<String> ignoreUris = Arrays.asList("/actuator/health");
    
    private List<PreFilterHandler> handlers = new ArrayList<>();
    
    public AbstracRequestFilter(PreFilterHandler...filterHandlers) {
		handlers.add(new GlobalHeaderHanlder());
		
		if(GatewayConfigs.actionLogEnabled) {
			handlers.add(new RequestLogHanlder());
		}
		
		if(GatewayConfigs.openApiEnabled) {
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
			ThreadLocalContext.unset();
			exchange.getAttributes().clear();
			if(e instanceof MendmixBaseException == false) {
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

	@Override
	public void doInitialize() {
		for (PreFilterHandler handler : handlers) {
			handler.onStarted();
		}
	}
    
    
}