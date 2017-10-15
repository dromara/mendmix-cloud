/**
 * 
 */
package com.jeesuite.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
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

	private static boolean inited;
	private static boolean merged;
	
	private final static Properties allProperties = new Properties();
	
	private static Method envHelperGetPropertiesMethod;
	
	private synchronized static void load() {
		if(inited)return;
		try {
        	Class<?> threadClazz = Class.forName("com.jeesuite.spring.helper.EnvironmentHelper");  
        	envHelperGetPropertiesMethod = threadClazz.getMethod("getProperty", String.class);
		} catch (Exception e) {}
		try {
            String classpath = System.getProperty("java.class.path");
            System.out.println("CLASSPATH: " + classpath);
			URL url = Thread.currentThread().getContextClassLoader().getResource("");
			if(url == null)url = ResourceUtils.class.getResource("");
			if(url == null)return;
			if (url.getProtocol().equals("file")) {				
				loadPropertiesFromFile(new File(url.getPath()));
			}else if (url.getProtocol().equals("jar")) {					
				loadPropertiesFromJarFile(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			inited = true;
		}
	}

	private static void loadPropertiesFromJarFile(URL url) throws UnsupportedEncodingException, IOException {
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
	
	
	private synchronized static void mergeWithEnvironment(){
		if(merged)return;
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		
		for (Entry<Object, Object> entry : entrySet) {
			Object value = null;
			try {
				value = envHelperGetPropertiesMethod.invoke(null, entry.getKey());
			} catch (Exception e) {
				return;
			}
			if(value == null)value = entry.getValue();
			if(value != null){
				allProperties.setProperty(entry.getKey().toString(), value.toString());
			}
		}
		
		merged = true;
	}

	/**
	 * 获取所有配置的副本
	 * @return
	 */
	public static Properties getAllProperties() {
		if(!inited){
			load();
		}
		if(!merged){
			mergeWithEnvironment();
		}
		return new Properties(allProperties);
	}
	
	@Deprecated
	public static String get(String key, String...defaultValue) {
		if (defaultValue != null && defaultValue.length > 0 && defaultValue[0] != null) {
			return getProperty(key,defaultValue[0]);
		} else {
			return getProperty(key);
		}
	}
	
	public static String getProperty(String key) {
		return getProperty(key, null);
	}
	
	public static String getAndValidateProperty(String key) {
		String value = getProperty(key, null);
		if(StringUtils.isBlank(value)){
			throw new IllegalArgumentException(String.format("Property for key:%s not exists", key));
		}
		return value;
	}

	public static String getProperty(String key, String defaultValue) {
		if(!inited){
			load();
		}
		if(!merged){
			mergeWithEnvironment();
		}
		if (allProperties.containsKey(key)) {
			return allProperties.getProperty(key);
		}
		
		try {
			if(envHelperGetPropertiesMethod != null){				
				Object _value = envHelperGetPropertiesMethod.invoke(null, key);
				if(_value != null){
					synchronized (allProperties) {						
						allProperties.setProperty(key, _value.toString());
					}
					return _value.toString();
				}
			}
		} catch (Exception e) {}
		
		String value = System.getProperty(key);
		if(StringUtils.isNotBlank(value))return value;
		
		value = System.getenv(key);
		if(StringUtils.isNotBlank(value))return value;
		
		return defaultValue;
	}
	
	public static int getInt(String key){
		return getInt(key,0);
	}
	
	public static int getInt(String key,int defalutValue){
		String v = getProperty(key);
		if(v != null)return Integer.parseInt(v);
		return defalutValue;
	}
	
	public static long getLong(String key){
		return getLong(key,0L);
	}
	
	public static long getLong(String key,long defalutValue){
		String v = getProperty(key);
		if(v != null)return Long.parseLong(v);
		return defalutValue;
	}
	
	public static boolean getBoolean(String key){
		return Boolean.parseBoolean(getProperty(key));
	}
	
	public synchronized static void merge(Properties properties){
		if(properties == null || properties.isEmpty())return;
		
        Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			Object value = null;
			try {value = envHelperGetPropertiesMethod.invoke(null, entry.getKey());} catch (Exception e) {}
			if(value == null)value = entry.getValue();
			if(value != null){
				allProperties.setProperty(entry.getKey().toString(), value.toString());
			}
		}
	}
	
	public synchronized static void add(String key,String value){
		if(StringUtils.isAnyBlank(key,value))return;
		allProperties.put(key, value);
	}
	
	public static boolean  containsProperty(String key){
		return allProperties.containsKey(key);
	}
	
}
