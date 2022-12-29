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
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.async.AsyncInitializer;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.post.ResponseBodyLogHandler;
import com.mendmix.gateway.filter.post.ResponseRewriteHandler;
import com.mendmix.gateway.filter.post.RewriteBodyServerHttpResponse;
import com.mendmix.gateway.filter.pre.GlobalHeaderHanlder;
import com.mendmix.gateway.filter.pre.OpenApiSignatureHandler;
import com.mendmix.gateway.filter.pre.RequestLogHanlder;
import com.mendmix.gateway.helper.RequestContextHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.spring.InstanceFactory;

import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public abstract class AbstracRouteFilter implements GlobalFilter, Ordered, CommandLineRunner, AsyncInitializer {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");

	private List<String> ignoreUris = Arrays.asList("/actuator/health");

	private List<PreFilterHandler> preHandlers = new ArrayList<>();
	private List<PostFilterHandler> postHandlers = new ArrayList<>();

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
			Builder requestBuilder = exchange.getRequest().mutate();
			for (PreFilterHandler handler : preHandlers) {
				requestBuilder = handler.process(exchange, module, requestBuilder);
			}
			//
			boolean loggingRespBody = GatewayConfigs.actionLogEnabled && !GatewayConfigs.actionResponseBodyIngore;
			boolean rewriteRespBody = GatewayConfigs.respRewriteEnabled && !GatewayConfigs.ignoreRewriteRoutes.contains(module.getRouteName());
			if(!loggingRespBody && !rewriteRespBody) {
				return chain.filter(exchange);
			}
			
			RewriteBodyServerHttpResponse newResponse = new RewriteBodyServerHttpResponse(exchange,module);
			final ServerWebExchange newExchange = exchange.mutate().request(requestBuilder.build()).response(newResponse).build();
			//
	    	return chain.filter(newExchange).then(Mono.fromRunnable(() -> {
				Long start = exchange.getAttribute(GatewayConstants.CONTEXT_REQUEST_START_TIME);
				if (logger.isDebugEnabled() && start != null) {
					logger.debug("MENDMIX-TRACE-LOGGGING-->> request_time_trace -> uri:{},useTime:{} ms" ,exchange.getRequest().getPath().value(),(System.currentTimeMillis() - start));
				}
			}));
		} catch (Exception e) {
			if (e instanceof MendmixBaseException == false) {
				logger.error("MENDMIX-TRACE-LOGGGING-->> requestFilter_error", e);
			}
			ServerHttpResponse response = exchange.getResponse();
			byte[] bytes = JsonUtils.toJson(WrapperResponse.fail(e)).getBytes(StandardCharsets.UTF_8);
			if (GlobalConstants.FEIGN_CLIENT
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
		// after AdaptCachedBodyGlobalFilter
		return Ordered.HIGHEST_PRECEDENCE + 1001;
	}

	@Override
	public void run(String... args) throws Exception {
		preHandlers.add(new GlobalHeaderHanlder());
		if (GatewayConfigs.actionLogEnabled) {
			preHandlers.add(new RequestLogHanlder());
		}
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
		if(GatewayConfigs.actionLogEnabled && !GatewayConfigs.actionResponseBodyIngore) {
			postHandlers.add(new ResponseBodyLogHandler());
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

}