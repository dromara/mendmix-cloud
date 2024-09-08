package org.dromara.mendmix.gateway.filter;

import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.springframework.web.server.ServerWebExchange;

/**
 * 
 * <br>
 * @author vakinge
 * @date 2023年7月21日
 */
public interface GrayRouteMatcher {
  
	static String  DEFAULT_GRAY_ROUTE = "http://default-gray-route";
	
	default String match(ServerWebExchange exchange,String systemId,BizSystemModule module,String tenantId,AuthUser user) {
		return match(systemId, module, tenantId, user);
	}
	
	/**
	 * 匹配灰度路由
	 * @param systemId
	 * @param module
	 * @param tenantId
	 * @param user
	 * @return 需要灰度的路由地址
	 */
	String match(String systemId,BizSystemModule module,String tenantId,AuthUser user);
}
