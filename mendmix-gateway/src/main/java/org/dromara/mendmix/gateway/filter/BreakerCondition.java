/**
 * 
 */
package org.dromara.mendmix.gateway.filter;

import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.springframework.web.server.ServerWebExchange;

/**
 * <br>
 * @author vakinge
 * @date 2024年4月9日
 */
public interface BreakerCondition {

	boolean match(ServerWebExchange exchange,BizSystemModule module,ApiInfo apiInfo);
}
