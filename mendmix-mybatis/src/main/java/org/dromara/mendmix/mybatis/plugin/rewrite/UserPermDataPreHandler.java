/**
 * 
 */
package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.List;

import org.dromara.mendmix.common.model.DataPermItem;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2024年4月10日
 */
public interface UserPermDataPreHandler {

	List<DataPermItem> handle(List<DataPermItem> items);
}
