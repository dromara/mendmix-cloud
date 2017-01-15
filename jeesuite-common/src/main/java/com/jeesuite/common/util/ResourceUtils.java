/**
 * 
 */
package com.jeesuite.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 资源文件加载工具类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2013年2月25日
 */
public class ResourceUtils {

	static boolean inited;
	
	static Map<String, String> propertiesMap = new HashMap<>();

	private synchronized static void load() {
		try {
            if(inited)return;
			File dir = new File(Thread.currentThread().getContextClassLoader().getResource("").getPath());
			loadPropertiesFromFile(dir);
			inited = true;
		} catch (Exception e) {
			inited = true;
			throw new RuntimeException(e);
		}
	}
	
	private static void loadPropertiesFromFile(File parent) throws FileNotFoundException, IOException{
		File[] files = parent.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				loadPropertiesFromFile(file);
			}else{
				String path = file.getPath();
				if(path.endsWith("properties")){
					if(path.contains("i18n"))continue;
					Properties p = new Properties();
					p.load(new FileReader(file));
					for(String key : p.stringPropertyNames()) { 
						String value = p.getProperty(key);
						if(value != null && !"".equals(value.toString().trim())){
							propertiesMap.put(key, value);
						}
					}
					System.out.println("load properties from file:" + path);
				}
			}
		}
	}

	public static String get(String key, String...defaultValue) {
		if(!inited){
			load();
		}
		if (propertiesMap.containsKey(key)) {
			return propertiesMap.get(key);
		}
		if (defaultValue != null && defaultValue.length > 0 && defaultValue[0] != null) {
			return defaultValue[0];
		} else {
			return System.getProperty(key);
		}
	}
	
	public static int getInt(String key,int defalutValue){
		String v = get(key);
		if(v != null)return Integer.parseInt(v);
		return defalutValue;
	}
	
	public static long getLong(String key,long defalutValue){
		String v = get(key);
		if(v != null)return Long.parseLong(v);
		return defalutValue;
	}
	
}
