package com.jeesuite.gateway.filter.pre;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.SimpleCryptUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.gateway.filter.FilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.springweb.client.RequestHeaderBuilder;


/**
 * 
 * 
 * <br>
 * Class Name   : GlobalHeaderHanlder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class GlobalHeaderHandler implements FilterHandler {

	@Override
	public ServerWebExchange process(ServerWebExchange exchange, BizSystemModule module) {
		
		Builder builder = exchange.getRequest().mutate();
        
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String requrstId = headers.getFirst(CustomRequestHeaders.HEADER_REQUEST_ID);
		if(requrstId == null) {
			requrstId = TokenGenerator.generate();
			builder.header(CustomRequestHeaders.HEADER_REQUEST_ID, requrstId);
		}
		
		builder.header(CustomRequestHeaders.HEADER_INVOKER_IS_GATEWAY, Boolean.TRUE.toString());

		RequestHeaderBuilder.getHeaders().forEach((k, v) -> {
			builder.header(k, v);
		});

		// 跨集群
		boolean crossCluster = false;
		try {
			String clusterName = headers.getFirst(CustomRequestHeaders.HEADER_CLUSTER_ID);
			if (clusterName != null) {
				clusterName = SimpleCryptUtils.decrypt(clusterName);
			}
			// TODO 验证合法性
			crossCluster = true;
		} catch (Exception e) {
		}

		if (!crossCluster) {
			// 一些header禁止前端传入
			if (headers.getFirst(CustomRequestHeaders.HEADER_IGNORE_TENANT) != null) {
				builder.headers(k -> k.remove(CustomRequestHeaders.HEADER_IGNORE_TENANT));
			}
			if (headers.getFirst(CustomRequestHeaders.HEADER_AUTH_USER) != null) {
				builder.headers(k -> k.remove(CustomRequestHeaders.HEADER_AUTH_USER));
				AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
				if (currentUser != null) {
					builder.header(CustomRequestHeaders.HEADER_AUTH_USER, currentUser.toEncodeString());
				}
			}
		}
        
		return exchange.mutate().request(builder.build()).build();
	}

	@Override
	public int order() {
		return 1;
	}

}
