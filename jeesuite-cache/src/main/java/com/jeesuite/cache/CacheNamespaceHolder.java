/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.cache;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : CacheNamespaceHolder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月9日
 */
public class CacheNamespaceHolder {

	private static final String CACHE_KEY_SPLITER = ":";
	public final static boolean namespaceEnabled = ResourceUtils.getBoolean("cache.namespace.enabled", false);
	private static ThreadLocal<String> holder = new ThreadLocal<>();
	
	public static String getNameSpace(){
		if(!namespaceEnabled)return null;
		return StringUtils.trimToEmpty(holder.get());
	}
	
	public static void setNamespace(String namespace){
		holder.set(namespace + CACHE_KEY_SPLITER);
	}
	
	public static void unset(){
		holder.remove();
	}
}
