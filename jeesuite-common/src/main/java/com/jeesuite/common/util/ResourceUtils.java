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
import java.util.Map;
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
	
	public static final String NULL_VALUE_PLACEHOLDER = "_NULL_PLACEHOLDER_";
	private static final String PLACEHOLDER_PREFIX = "${";
	private static final String PLACEHOLDER_SUFFIX = "}";

	private static boolean inited;
	private static boolean merged;
	
	private final static Properties allProperties = new Properties();
	
	private static Method envHelperGetPropertiesMethod;
	private static Method envHelperGetAllPropertiesMethod;
	
	private synchronized static void load() {
		if(inited)return;
		try {
        	Class<?> threadClazz = Class.forName("com.jeesuite.spring.helper.EnvironmentHelper");  
        	envHelperGetPropertiesMethod = threadClazz.getMethod("getProperty", String.class);
        	envHelperGetAllPropertiesMethod = threadClazz.getMethod("getAllProperties", String.class);
		} catch (Exception e) {}
		
		merged = envHelperGetAllPropertiesMethod == null && envHelperGetPropertiesMethod == null;
		try {
			String extPropertyDir = System.getProperty("ext.config.dir");
			if(StringUtils.isNotBlank(extPropertyDir)){
				File file = new File(extPropertyDir);
				if(file.exists()){
					loadPropertiesFromFile(file);
				}
			}
            System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));
			URL url = Thread.currentThread().getContextClassLoader().getResource("");
			 System.setProperty("framework.website", "www.jeesuite.com");
			if(url == null)url = ResourceUtils.class.getResource("");
			if(url == null)return;
			if (url.getProtocol().equals("file")) {	
				System.out.println("loadPropertiesFromFile,origin:"+url.getPath());
				File parent = new File(url.getPath());
				if(!parent.exists()){
					System.err.println("loadPropertiesFromFile_error,dir not found");
				}else{					
					loadPropertiesFromFile(parent);
				}
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
		
		System.out.println("loadPropertiesFromJarFile,origin:" + url.toString());
		String jarFilePath = url.getFile();	
		if(jarFilePath.contains("war!")){
			jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "war!")[0] + "war";
		}else if(jarFilePath.contains("jar!")){
			jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "jar!")[0] + "jar";
		}
		jarFilePath = jarFilePath.substring("file:".length());
		jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
		System.out.println("loadPropertiesFromJarFile,real:" + jarFilePath);
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
		if(files == null)return;
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
	
	
	@SuppressWarnings("unchecked")
	private synchronized static void mergeWithEnvironment(){
		if(merged)return;
		Map<String, Object> envProperties = null;
		if(envHelperGetAllPropertiesMethod != null){
			try {
				envProperties = (Map<String, Object>) envHelperGetAllPropertiesMethod.invoke(null,"");
				if(envProperties == null || envProperties.isEmpty())return;
				for (String key : envProperties.keySet()) {
					allProperties.setProperty(key, envProperties.get(key).toString());
				}
				merged = true;
			} catch (Exception e) {}
			return;
		}
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		
		for (Entry<Object, Object> entry : entrySet) {
			Object value = null;
			try {
				value = envHelperGetPropertiesMethod.invoke(null, entry.getKey());
				if(value != null){
					allProperties.setProperty(entry.getKey().toString(), value.toString());
				}
			} catch (Exception e) {
				return;
			}
		}
		
		merged = true;
	}

	/**
	 * 获取所有配置的副本
	 * @return
	 */
	public static Properties getAllProperties() {
		return getAllProperties(null);
	}
	
	/**
	 * 获取指定前缀的配置
	 */
	public static Properties getAllProperties(String prefix) {
		if(!inited){
			load();
		}
		if(!merged){
			mergeWithEnvironment();
		}
		
		Properties properties = new Properties();
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			if(StringUtils.isBlank(prefix) || entry.getKey().toString().startsWith(prefix)){				
				properties.put(entry.getKey(), entry.getValue());
			}
		}
		return properties;
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
		
		//优先环境变量
		String value = System.getProperty(key);
		if(StringUtils.isNotBlank(value))return value;
		
		value = System.getenv(key);
		if(StringUtils.isNotBlank(value))return value;
		
		if (allProperties.containsKey(key)) {
			return allProperties.getProperty(key);
		}
		
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
	
	public static boolean getBoolean(String key,boolean defalutValue){
		return containsProperty(key) ? Boolean.parseBoolean(getProperty(key)) : defalutValue;
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
		addToProperties(key, value);
	}
	
	public static boolean  containsProperty(String key){
		return allProperties.containsKey(key);
	}
	
	/**
	 * 如果替换包含占位符则替换占位符
	 * @param key
	 * @return
	 */
    private static String addToProperties(String key,String value ) {
		
    	if(!value.contains(PLACEHOLDER_PREFIX)){
    		allProperties.put(key, value);
    		return value;
    	}
    	
    	String[] segments = value.split("\\$\\{");
		String seg;
		
		StringBuilder finalValue = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			seg = StringUtils.trimToNull(segments[i]);
			if(StringUtils.isBlank(seg))continue;
			
			if(seg.contains(PLACEHOLDER_SUFFIX)){	
				String refKey = seg.substring(0, seg.indexOf(PLACEHOLDER_SUFFIX)).trim();
				//其他非${}的占位符如：{{host}}
				String withBraceString = null;
				if(seg.contains("{")){
					withBraceString = seg.substring(seg.indexOf(PLACEHOLDER_SUFFIX)+1);
				}
				
				//如果包含默认值，如：${host:127.0.0.1}
				String defaultValue = null;
				if(refKey.contains(":")){
					String[] tmpArray = refKey.split(":");
					refKey = tmpArray[0];
					defaultValue = tmpArray[1];
				}
				
				String refValue = getProperty(refKey);
				if(StringUtils.isBlank(refValue)){
					refValue = defaultValue;
				}
				finalValue.append(refValue);
				
				if(withBraceString != null){
					finalValue.append(withBraceString);
				}else{
					String[] segments2 = seg.split("\\}");
					if(segments2.length == 2){
						finalValue.append(segments2[1]);
					}
				}
			}else{
				finalValue.append(seg);
			}
		}
		
		allProperties.put(key, finalValue.toString());
		
		return finalValue.toString();
	}
	
}
