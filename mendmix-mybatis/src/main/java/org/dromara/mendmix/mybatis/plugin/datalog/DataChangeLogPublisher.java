/**
 * 
 */
package org.dromara.mendmix.mybatis.plugin.datalog;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年12月12日
 */
public interface DataChangeLogPublisher {
	boolean publish(DataChangeItem item);
}
