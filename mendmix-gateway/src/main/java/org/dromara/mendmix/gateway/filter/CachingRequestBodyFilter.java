/**
 * 
 */
package org.dromara.mendmix.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;

import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * from AdaptCachedBodyGlobalFilter
 * <br>
 * @author vakinge
 * @date 2023年8月14日
 */
public class CachingRequestBodyFilter implements WebFilter {

	private static final String HTTP = "http";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if(request.getMethod() == HttpMethod.GET || RequestContextHelper.isMultipartContent(request)) {
			return chain.filter(exchange);
		}
		
		String scheme = request.getURI().getScheme();
		// Record only http requests (including https)
		if ((!scheme.startsWith(HTTP))) {
			return chain.filter(exchange);
		}

		ServerHttpRequest cachedRequest = exchange.getAttributeOrDefault(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR,null);
		if (cachedRequest != null) {
			exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
			return chain.filter(exchange.mutate().request(cachedRequest).build());
		}

		//
		DataBuffer body = exchange.getAttributeOrDefault(CACHED_REQUEST_BODY_ATTR, null);
		if (body != null) {
			return chain.filter(exchange);
		}

		RequestContextHelper.clearContextAttributes(exchange);
		RequestContextHelper.setContextAttr(exchange,GatewayConstants.CONTEXT_CLEARED_CONTEXT_ATTR, true);
		return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
			// don't mutate and build if same request object
			if (serverHttpRequest == exchange.getRequest()) {
				return chain.filter(exchange);
			}
			return chain.filter(exchange.mutate().request(serverHttpRequest).build());
		});
	}

}
