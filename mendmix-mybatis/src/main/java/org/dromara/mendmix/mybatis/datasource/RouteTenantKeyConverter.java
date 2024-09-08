package org.dromara.mendmix.mybatis.datasource;

/**
 * 租户路由key转换
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年3月14日
 */
public interface RouteTenantKeyConverter {

	/**
	 * 租户id转租户编码
	 * @param group
	 * @param tenantId
	 * @return
	 */
	String convert(String group,String tenantId);
	
	default boolean caching() {return false;}
}
