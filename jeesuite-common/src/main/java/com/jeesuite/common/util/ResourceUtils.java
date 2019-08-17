package com.jeesuite.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
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
	public static final String PLACEHOLDER_PREFIX = "${";
	public static final String PLACEHOLDER_SUFFIX = "}";

	private static boolean inited;
	private static boolean merged;
	
	private final static Properties allProperties = new Properties();
	
	private static Method getAllPropertiesFromEnvMethod;
	
	private synchronized static void load() {
		if(inited)return;
		try {
        	Class<?> threadClazz = Class.forName("com.jeesuite.spring.helper.EnvironmentHelper");  
        	getAllPropertiesFromEnvMethod = threadClazz.getMethod("getAllProperties", String.class);
		} catch (Exception e) {}
		merged = getAllPropertiesFromEnvMethod == null;
		try {
			String extPropertyDir = System.getProperty("ext.config.dir");
			if(StringUtils.isNotBlank(extPropertyDir)){
				File file = new File(extPropertyDir);
				if(file.exists()){
					loadPropertiesFromFile(file);
				}
			}
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
			
			inited = true;
			//
			List<Object> keys = new ArrayList<>(allProperties.keySet());
			for (Object key : keys) {
				if(allProperties.getProperty(key.toString()).contains(PLACEHOLDER_PREFIX)){
					String value = replaceRefValue(allProperties.getProperty(key.toString()));
					if(StringUtils.isNotBlank(value))allProperties.setProperty(key.toString(), value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		
		List<String> fileList = new ArrayList<>();
		String fileExt = null;
		
		Enumeration<JarEntry> entries = jarFile.entries(); 
		while (entries.hasMoreElements()) {  
			JarEntry entry = entries.nextElement();
			if(entry.getName().endsWith(".properties")){
				if(entry.getName().contains("i18n"))continue;
				fileList.add(entry.getName());
				if(fileExt == null)fileExt = ".properties";
			}
		} 
		
		parseConfigSortFiles(fileList, fileExt,jarFile);
		jarFile.close();
	}
	
	private static void loadPropertiesFromFile(File parent) throws FileNotFoundException, IOException{
		File[] files = parent.listFiles();
		if(files == null)return;
		List<String> fileList = new ArrayList<>();
		String fileExt = null;
		for (File file : files) {
			if(file.isDirectory()){
				loadPropertiesFromFile(file);
			}else{
				String path = file.getPath();
				if(path.endsWith("properties")){
					if(path.contains("i18n"))continue;
					fileList.add(path);
					if(fileExt == null)fileExt = ".properties";
				}
			}
		}
		
		parseConfigSortFiles(fileList, fileExt,null);
		
	}

	/**
	 * @param fileList
	 * @param fileExt
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static void parseConfigSortFiles(List<String> fileList, String fileExt,JarFile jarFile)
			throws IOException, FileNotFoundException {
		if(fileList.size() == 1){
			Properties p = parseToProperties(fileList.get(0), jarFile);
			allProperties.putAll(p);
		}else if(fileList.size() > 1){
			sortFileNames(fileList, fileExt);
			Map<String, Properties> filePropMap = new LinkedHashMap<>(fileList.size());
			
			String withProfileKeyFile = null;
			String profileFileBaseName = null;
			String activeProfileFile = null;
			Properties p;
			for (String file : fileList) {
				p = parseToProperties(file, jarFile);
				if(p.containsKey("spring.profiles.active")){
					withProfileKeyFile = file;
					profileFileBaseName = file.replace(fileExt, "") + "-";
					activeProfileFile = profileFileBaseName + p.getProperty("spring.profiles.active") + fileExt;
				}
				filePropMap.put(file, p);
			}
			
			for (String filePath : filePropMap.keySet()) {
				if(profileFileBaseName == null 
						|| filePath.equals(withProfileKeyFile)
						|| !filePath.startsWith(profileFileBaseName)
						|| filePath.equals(activeProfileFile)){
					allProperties.putAll(filePropMap.get(filePath));
					System.out.println("load properties from file:" + filePath);
				}
			}
		}
	}
	
	private static Properties parseToProperties(String path,JarFile jarFile) throws FileNotFoundException, IOException{
		Properties properties = new Properties();
		if(jarFile == null){
			properties.load(new FileReader(path));
		}else{
			InputStream inputStream = jarFile.getInputStream(jarFile.getJarEntry(path));
			properties.load(inputStream);
			try {inputStream.close();} catch (Exception e) {}
		}
		return properties;
	}
	
	private static void sortFileNames(List<String> files,String ext){
		Collections.sort(files, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				o1 = o1.replace(ext, "");
				o2 = o2.replace(ext, "");
				return o1.compareTo(o2);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private synchronized static void mergeWithEnvironment(){
		if(merged)return;
		Map<String, Object> envProperties = null;
		if(getAllPropertiesFromEnvMethod != null){
			try {
				envProperties = (Map<String, Object>) getAllPropertiesFromEnvMethod.invoke(null,"");
				for (String key : envProperties.keySet()) {
					if(envProperties.get(key) != null){
						allProperties.setProperty(key, envProperties.get(key).toString());
					}
				}
				merged = true;
			} catch (Exception e) {}
		}
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
				String value = replaceRefValue(entry.getValue().toString());
				properties.put(entry.getKey(), value);
			}
		}
		return properties;
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
		
		value = allProperties.getProperty(key);
		if (StringUtils.isNotBlank(value)) {
			value = replaceRefValue(value);
			return value;
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
			Object value = entry.getValue();
			if(value != null){
				String refValue = replaceRefValue(value.toString());
				if(StringUtils.isNotBlank(refValue)){
					allProperties.setProperty(entry.getKey().toString(), refValue);
				}
			}
		}
	}
	
	public synchronized static void merge(Map<String, Object> properties){
		for (String key : properties.keySet()) {
			String refValue = replaceRefValue(properties.get(key).toString());
			if(StringUtils.isNotBlank(refValue)){
				allProperties.setProperty(key, refValue);
			}
		}
	}
	
	public synchronized static void add(String key,String value){
		if(StringUtils.isAnyBlank(key,value))return;
		value = replaceRefValue(value);
		if(StringUtils.isNotBlank(value))allProperties.setProperty(key, value);
	}
	
	public static boolean  containsProperty(String key){
		if(System.getProperties().containsKey(key))return true;
		if(System.getenv().containsKey(key))return true;
		return allProperties.containsKey(key);
	}
	
	/**
	 * 如果替换包含占位符则替换占位符
	 * @param key
	 * @return
	 */
    public static String replaceRefValue(Properties properties,String value ) {
		
    	if(!value.contains(PLACEHOLDER_PREFIX)){
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
				int defaultValSpliterIndex = refKey.indexOf(":");
				if(defaultValSpliterIndex > 0){
					defaultValue = refKey.substring(defaultValSpliterIndex + 1);
					refKey = refKey.substring(0,defaultValSpliterIndex);
				}
				
				String refValue = System.getProperty(refKey);
				if(StringUtils.isBlank(refValue))refValue = System.getenv(refKey);
				if(StringUtils.isBlank(refValue))refValue = properties.getProperty(refKey);
				if(StringUtils.isBlank(refValue)){
					refValue = defaultValue;
				}
				
				if(StringUtils.isBlank(refValue)){
					finalValue.append(PLACEHOLDER_PREFIX + refKey + PLACEHOLDER_SUFFIX);
				}else{
					finalValue.append(refValue);
				}
				
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
		
		return finalValue.toString();
	}
    
    private static String replaceRefValue(String value){
    	return replaceRefValue(allProperties, value);
    }
	
    public static void main(String[] args) {
    	allProperties.setProperty("spring.application.name", "user");
    	String prop = "${baseUrl:http://localhost:8080}/api";
    	
    	System.out.println(replaceRefValue(prop));
    }
}
