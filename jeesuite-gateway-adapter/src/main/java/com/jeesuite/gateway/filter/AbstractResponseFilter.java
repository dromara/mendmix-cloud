package com.jeesuite.gateway.filter;

import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.model.WrapperResponse;
import com.jeesuite.common.util.JsonUtils;

import reactor.core.publisher.Mono;

public abstract class AbstractResponseFilter implements GlobalFilter, Ordered, InitializingBean {

	static Logger logger = LoggerFactory.getLogger("com.jeesuite.gateway");

	private GatewayFilter delegate;

	@Autowired
	private ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		System.out.println("--->>RewriteResponseFilter");
		return delegate.filter(exchange, chain);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		delegate = modifyResponseBodyGatewayFilterFactory.apply(
			new ModifyResponseBodyGatewayFilterFactory.Config() //
				.setRewriteFunction(new BodyRewriteFunction())  //
				.setInClass(byte[].class)  //
				.setOutClass(byte[].class) //
			);
	}

	@Override
	public int getOrder() {
		return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
	}

	private static class BodyRewriteFunction implements RewriteFunction<byte[], byte[]> {

		@Override
		public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] bytes) {

			if (ServerWebExchangeUtils.isAlreadyRouted(exchange)) {
				WrapperResponse<Object> wrapperResponse = new WrapperResponse<>();
				wrapperResponse.setData(JsonUtils.toHashMap(new String(bytes, StandardCharsets.UTF_8), Object.class));
				return Mono.just(JsonUtils.toJson(wrapperResponse).getBytes());
			}
			return Mono.just(bytes);
		}
	}

}