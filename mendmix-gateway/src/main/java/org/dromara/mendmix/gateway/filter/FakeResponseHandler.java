package org.dromara.mendmix.gateway.filter;

import org.springframework.web.server.ServerWebExchange;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月19日
 */
public interface FakeResponseHandler {

	Object handle(ServerWebExchange exchange,boolean preHandle);
	
	int order();
}
