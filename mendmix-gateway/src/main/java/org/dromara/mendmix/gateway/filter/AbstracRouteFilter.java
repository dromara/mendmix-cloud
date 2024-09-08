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
package org.dromara.mendmix.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.AsyncInitializer;
import org.dromara.mendmix.common.model.WrapperResponse;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.filter.post.ResponseRewriteHandler;
import org.dromara.mendmix.gateway.filter.post.RewriteBodyServerHttpResponse;
import org.dromara.mendmix.gateway.filter.pre.GlobalHeaderHanlder;
import org.dromara.mendmix.gateway.filter.pre.OpenApiSignatureHandler;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public abstract class AbstracRouteFilter implements GlobalFilter, Ordered, CommandLineRunner, AsyncInitializer {

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.gateway");

	private List<String> ignoreUris = Arrays.asList("/actuator/health");

	private List<PreFilterHandler> preHandlers = new ArrayList<>();
	private List<PostFilterHandler> postHandlers = new ArrayList<>();
	
	@Autowired(required = false)
    private List<FakeResponseHandler> fakeResponseHandlers;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		
		if(exchange.getAttribute(GatewayConstants.CONTEXT_IGNORE_FILTER) != null) {
    		return chain.filter(exchange);
    	}
		
		String requestUri = exchange.getRequest().getPath().value();
		if (ignoreUris.stream().anyMatch(o -> requestUri.endsWith(o))) {
			return chain.filter(exchange);
		}
		
		exchange.getAttributes().put(GatewayConstants.CONTEXT_REQUEST_START_TIME, System.currentTimeMillis());

		BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		try {
			Object fakeResp = null;
    		if(fakeResponseHandlers != null) {
    			for (FakeResponseHandler handler : fakeResponseHandlers) {
    				fakeResp = handler.handle(exchange,true);
    				if(fakeResp != null)break;
				}
    			if(fakeResp != null) {
    				if(fakeResp instanceof Map) {
        				if(((Map<String, Object>) fakeResp).containsKey(GatewayConstants.NULL_BODY_KEY)) {
        					fakeResp = null;
        				}else if(GlobalConstants.FEIGN_USER_AGENT_NAME.equals(exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT))) {
            				fakeResp = ((Map<String, Object>) fakeResp).get(GlobalConstants.PARAM_DATA);
            			}
        			}
        			if(fakeResp == null)return Mono.empty();
        			byte[] fallbackRespBytes = JsonUtils.toJsonBytes(fakeResp);
        			return RequestContextHelper.writeData(exchange, fallbackRespBytes, 200);
    			}
    		}	
			//
			Builder requestBuilder = exchange.getRequest().mutate();
			for (PreFilterHandler handler : preHandlers) {
				requestBuilder = handler.process(exchange, module, requestBuilder);
			}
			exchange = exchange.mutate().request(requestBuilder.build()).build();
    		//
    		boolean rewritePath = StringUtils.isNotBlank(module.getRewriteUriPrefix());
    		String rewriteBaseUrl = null; 
    		if(rewritePath) {
        		RequestContextHelper.rewriteRouteUri(exchange, module, rewriteBaseUrl);
        		RequestContextHelper.setContextAttr(exchange, GatewayConstants.CONTEXT_CROSS_CLOUD_REQUEST, true);
        	}
    		//
    		if(logger.isTraceEnabled() || exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME)) {
    			logger.info("<trace_logging> final_forward_request \n - uri:{}\n - headers:{}"
    					,Objects.toString(exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR))
    					,exchange.getRequest().getHeaders());
    		}
    		//
    		org.springframework.web.server.ServerWebExchange.Builder newExchangeBuilder = exchange.mutate().request(requestBuilder.build());
    		boolean rewriteBody = true; //	TODO 
    		if(rewriteBody) {
    			RewriteBodyServerHttpResponse newResponse = new RewriteBodyServerHttpResponse(exchange,module);
    			exchange = newExchangeBuilder.response(newResponse).build();
        		return chain.filter(exchange.mutate().response(newResponse).build());
    		}
    		exchange = newExchangeBuilder.build();
    		return chain.filter(exchange);
		} catch (Exception e) {
			if (e instanceof MendmixBaseException == false) {
				logger.error("MENDMIX-TRACE-LOGGGING-->> requestFilter_error", e);
			}
			ServerHttpResponse response = exchange.getResponse();
			byte[] bytes = JsonUtils.toJson(WrapperResponse.fail(e)).getBytes(StandardCharsets.UTF_8);
			if (GlobalConstants.FEIGN_USER_AGENT_NAME
					.equalsIgnoreCase(exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT))) {
				response.setRawStatusCode(500);
			}
			response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
			return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
		}finally {
			ThreadLocalContext.unset();
		}
	}

	@Override
	public int getOrder() {
		// after NettyWriteResponseFilter
		return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
	}

	@Override
	public void run(String... args) throws Exception {
		preHandlers.add(new GlobalHeaderHanlder());
		if (GatewayConfigs.openApiEnabled) {
			preHandlers.add(new OpenApiSignatureHandler());
		}
		Map<String, PreFilterHandler> customPreHandlers = InstanceFactory.getBeansOfType(PreFilterHandler.class);
		if(!customPreHandlers.isEmpty()) {
			preHandlers.addAll(customPreHandlers.values());
		}
		if (preHandlers.size() > 1) {
			preHandlers = preHandlers
					.stream()
					.sorted(Comparator.comparing(PreFilterHandler::order))
					.collect(Collectors.toList());
		}
		//
		if(GatewayConfigs.respRewriteEnabled) {
			postHandlers.add(new ResponseRewriteHandler());
		}
		Map<String, PostFilterHandler> customPostHandlers = InstanceFactory.getBeansOfType(PostFilterHandler.class);
		if(!customPostHandlers.isEmpty()) {
			postHandlers.addAll(customPostHandlers.values());
		}
		RewriteBodyServerHttpResponse.setHandlers(postHandlers);

	}

	@Override
	public void doInitialize() {
		for (PreFilterHandler handler : preHandlers) {
			handler.onStarted();
		}
		//
		for (PostFilterHandler handler : postHandlers) {
			handler.onStarted();
		}
		
	}
	
	public static void addIgnoreResponseFilterUri(ServerWebExchange exchange) {
		
	}

}