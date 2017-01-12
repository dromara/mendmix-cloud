/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import com.jeesuite.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月12日
 */
public class DefaultConfigReader implements ConfigReader {

	
	@Override
	public String get(String key) {
		return ResourceUtils.get(key);
	}
	
	@Override
	public String getIfAbent(String key, Object defaulttVal) {
		if(defaulttVal == null)return get(key);
		return ResourceUtils.get(key, defaulttVal.toString());
	}

	@Override
	public boolean containKey(String key) {
		return get(key) != null;
	}

}
