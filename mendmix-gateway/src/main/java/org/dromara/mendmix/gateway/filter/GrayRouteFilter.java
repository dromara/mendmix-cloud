package org.dromara.mendmix.gateway.filter;

import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.http.HostMappingHolder;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 灰度路由
 * <br>
 * @author vakinge
 * @date 2023年7月21日
 */
public class GrayRouteFilter implements GlobalFilter, Ordered{

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");
	
	private Map<String, String> grayRouteMappings = ResourceUtils.getMappingValues("mendmix-cloud.servicegovern.grayRoute.mapping");
	
	@Autowired(required = false)
	private GrayRouteMatcher grayRouteMatcher;
	
	@Override
	public int getOrder() {
		return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if(grayRouteMatcher == null && grayRouteMappings.isEmpty()) {
			return chain.filter(exchange);
		}
		BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		String systemId = CurrentRuntimeContext.getSystemId();
		String tenantId = CurrentRuntimeContext.getTenantId();
		AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
		
		String grayRouteUrl = null;
		boolean usingGrayRoute = exchange.getRequest().getHeaders().containsKey(CustomRequestHeaders.HEADER_USING_GRAY_STRATEGY);
		if(!usingGrayRoute && grayRouteMatcher != null) {
			usingGrayRoute = (grayRouteUrl = grayRouteMatcher.match(exchange,systemId, module, tenantId, authUser)) != null;
		}
		
		if(!usingGrayRoute) {
			return chain.filter(exchange);
		}
		boolean defaultRouteMatched = false;
		if(grayRouteUrl == null || (defaultRouteMatched = GrayRouteMatcher.DEFAULT_GRAY_ROUTE.equals(grayRouteUrl))) {
			grayRouteUrl = grayRouteMappings.get(module.getServiceId());
			if(grayRouteUrl == null) {
				grayRouteUrl = grayRouteMappings.get(module.getCurrentRouteName(exchange));
			}
			if(defaultRouteMatched && grayRouteUrl == null) {
				logger.debug(">>grayRoute for:{} NOT FOUND!!!",exchange.getRequest().getPath().value());
			}
		}
		if(StringUtils.isBlank(grayRouteUrl)) {
			return chain.filter(exchange); 
		}
		//兼容负载均衡地址
		if(!grayRouteUrl.contains(GlobalConstants.DOT)) {
			grayRouteUrl = HostMappingHolder.resolveUrl(grayRouteUrl);
		}
		//重写转发uri
		URI grayRouteUri = RequestContextHelper.rewriteRouteUri(exchange, module, grayRouteUrl);
		if(logger.isTraceEnabled() || exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME)) {
			logger.info(">> GrayRouteRequest grayRouteUri:{}",grayRouteUri);
		}
		exchange.getAttributes().put(GatewayConstants.CONTEXT_GRAY_ROUTED, true);
		return chain.filter(exchange);
	}


}
