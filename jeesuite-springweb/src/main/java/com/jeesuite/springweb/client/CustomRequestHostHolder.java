package com.jeesuite.springweb.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : CustomRequestHostResolver
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年12月06日
 */
public class CustomRequestHostHolder {

	private static Map<String, String> baseNameMappings = new HashMap<>();
	
	private static Map<String, String> getBaseNameMappings() {
		if(!baseNameMappings.isEmpty())return baseNameMappings;
		synchronized (baseNameMappings) {
			if(!baseNameMappings.isEmpty())return baseNameMappings;
			Properties properties = ResourceUtils.getAllProperties("remote.baseurl.mapping");
			properties.forEach((k,v) -> {
				String[] parts = k.toString().split("\\[|\\]");
				String lbBaseUrl = parts[1];
				if(parts.length >= 4){
					lbBaseUrl = lbBaseUrl + ":" + parts[3];
				}
				baseNameMappings.put(lbBaseUrl, v.toString().replace("http://", ""));
			});
			if(baseNameMappings.isEmpty()){
				baseNameMappings.put("x", "0");
			}
		}
		return baseNameMappings;
	}
	
	public static String getMapping(String name){
		return getBaseNameMappings().get(name);
	}
	
	public static String resolveUrl(String url){
		String lbBaseUrl = StringUtils.split(url, "/")[1];
		Map<String, String> baseNameMappings = getBaseNameMappings();
		if(baseNameMappings.containsKey(lbBaseUrl)){
			return url.replace(lbBaseUrl, baseNameMappings.get(lbBaseUrl));
		}
		return url;
	}
}
