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
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;

/**
 * 资源文件加载工具类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2013年2月25日
 */
public final class ResourceUtils {

	static boolean inited;
	
	final static Properties allProperties = new Properties();

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
			        		allProperties.putAll(properties);
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
					allProperties.putAll(p);
					System.out.println("load properties from file:" + path);
				}
			}
		}
	}
	

	/**
	 * 获取所有配置的副本
	 * @return
	 */
	public static Properties getAllProperties() {
		return new Properties(allProperties);
	}
	
	public static String get(String key) {
		return get(key, null);
	}

	public static String get(String key, String defaultValue) {
		if(!inited){
			load();
		}
		if (allProperties.containsKey(key)) {
			return allProperties.getProperty(key);
		}
		
		String value = System.getProperty(key);
		if(StringUtils.isNotBlank(value))return value;
		return defaultValue;
	}
	
	public static int getInt(String key){
		return getInt(key,0);
	}
	
	public static int getInt(String key,int defalutValue){
		String v = get(key);
		if(v != null)return Integer.parseInt(v);
		return defalutValue;
	}
	
	public static long getLong(String key){
		return getLong(key,0L);
	}
	
	public static long getLong(String key,long defalutValue){
		String v = get(key);
		if(v != null)return Long.parseLong(v);
		return defalutValue;
	}
	
	public static boolean getBoolean(String key){
		return Boolean.parseBoolean(get(key));
	}
	
	public synchronized static void merge(Properties properties){
		if(properties == null || properties.isEmpty())return;
		allProperties.putAll(properties);
	}
	
	public synchronized static void add(String key,String value){
		if(StringUtils.isAnyBlank(key,value))return;
		allProperties.put(key, value);
	}
	
	public static boolean  containsProperty(String key){
		return allProperties.containsKey(key);
	}
	
}
