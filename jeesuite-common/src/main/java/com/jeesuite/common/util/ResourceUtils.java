/**
 * 
 */
package com.jeesuite.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
			URL url = Thread.currentThread().getContextClassLoader().getResource("");
			if (url.getProtocol().equals("file")) {				
				loadPropertiesFromFile(new File(url.getPath()));
			}else if (url.getProtocol().equals("jar")) {
				try {					
					String jarFilePath = url.getFile();	
					jarFilePath = jarFilePath.split("jar!")[0] + "jar";
					jarFilePath = jarFilePath.substring("file:".length());
					jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
					JarFile jarFile = new JarFile(jarFilePath);
					
					Enumeration<JarEntry> entries = jarFile.entries(); 
			        while (entries.hasMoreElements()) {  
			        	JarEntry entry = entries.nextElement();
			        	if(entry.getName().endsWith(".properties")){
			        		InputStream inputStream = jarFile.getInputStream(jarFile.getJarEntry(entry.getName()));
			        		Properties properties = new Properties();
			        		properties.load(inputStream);
			        		try {inputStream.close();} catch (Exception e) {}
			        		for(String key : properties.stringPropertyNames()) { 
			        			String value = properties.getProperty(key);
			        			if(value != null && !"".equals(value.toString().trim())){
			        				propertiesMap.put(key, value);
			        			}
			        		}
			        	}
			 
			        } 
			        jarFile.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			}
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
	

	/**
	 * 获取所有配置的副本
	 * @return
	 */
	public static Map<String, String> getAllProperties() {
		return new HashMap<>(propertiesMap);
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
	
	public void add(String key,String value){
		propertiesMap.put(key, value);
	}
	
}
