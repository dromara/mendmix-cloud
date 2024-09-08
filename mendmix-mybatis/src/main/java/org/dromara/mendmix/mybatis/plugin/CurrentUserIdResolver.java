package org.dromara.mendmix.mybatis.plugin;

import org.dromara.mendmix.common.model.AuthUser;

/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年6月5日
 */
public interface CurrentUserIdResolver {

	String resolve(AuthUser currentUser);
}
