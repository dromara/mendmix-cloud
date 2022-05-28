/*
 * Copyright 2016-2020 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * 资源文件加载工具类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2013年2月25日
 */
public final class ResourceUtils {
	
	
	public static String CONFIG_DELIMITERS = ",; \t\n";
	public static final String NULL_VALUE_PLACEHOLDER = "_NULL_PLACEHOLDER_";
	public static final String PLACEHOLDER_PREFIX = "${";
	public static final String PLACEHOLDER_SUFFIX = "}";

	private static String profile;
	private static String profileFile;
	private static Properties profileProperties;
	
	private final static Properties allProperties = new Properties();
	
	
	static {
		loadLocalConfigs();
	}
	
	private static void loadLocalConfigs() {
		try {
			profile = System.getProperty("spring.profiles.active");
			URL url = Thread.currentThread().getContextClassLoader().getResource("");
			if(url == null)url = ResourceUtils.class.getResource("");
			
			Map<String, List<String>> allFileMap = new HashMap<>();
			if(url != null){
				if (url.getProtocol().equals("file")) {	
					File parent = new File(url.getPath());
					if(!parent.exists()){
						System.err.println(">>loadPropertiesFromFile_error,dir not found");
					}else{					
						loadPropertiesFromFile(parent);
					}
				}else if (url.getProtocol().equals("jar")) {					
					loadPropertiesFromJarFile(url,allFileMap);
				}
			}
			//
			List<Object> keys = new ArrayList<>(allProperties.keySet());
			for (Object key : keys) {
				if(key == null || allProperties.getProperty(key.toString()) == null)continue;
				if(allProperties.getProperty(key.toString()).contains(PLACEHOLDER_PREFIX)){
					String value = replaceRefValue(allProperties.getProperty(key.toString()));
					if(StringUtils.isNotBlank(value))allProperties.setProperty(key.toString(), value);
				}
			}
			
			if(profileFile != null) {
				allProperties.putAll(profileProperties);
				System.out.println(">>load properties from file:" + profileFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadPropertiesFromJarFile(URL url,Map<String, List<String>> allFileMap) throws UnsupportedEncodingException, IOException {
		
		String jarFilePath = url.getFile();	
		if(jarFilePath.contains("war!")){
			jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "war!")[0] + "war";
		}else if(jarFilePath.contains("jar!")){
			jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "jar!")[0] + "jar";
		}
		jarFilePath = jarFilePath.substring("file:".length());
		jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
		JarFile jarFile = new JarFile(jarFilePath);
		
		String fileExt = null;
		Enumeration<JarEntry> entries = jarFile.entries(); 
		while (entries.hasMoreElements()) {  
			JarEntry entry = entries.nextElement();
			if(entry.getName().endsWith(".properties") || entry.getName().endsWith(".yml") || entry.getName().endsWith(".yaml")){
				if(entry.getName().contains("/i18n/"))continue;
				if(entry.getName().endsWith("pom.properties"))continue;
				fileExt = entry.getName().substring(entry.getName().lastIndexOf("."));
				if(!allFileMap.containsKey(fileExt)){
					allFileMap.put(fileExt, new ArrayList<String>());
				}
				allFileMap.get(fileExt).add(entry.getName());
			}
		} 
		
		Set<String> fileExts = allFileMap.keySet();
		for (String key : fileExts) {
			parseConfigSortFiles(allFileMap.get(key), key, jarFile);
		}
		
		jarFile.close();
	}
	
	private static void loadPropertiesFromFile(File parent) throws FileNotFoundException, IOException{
		Map<String, List<String>> allFileMap = new HashMap<>();
		File[] files = parent.listFiles();
		if(files == null)return;
		String fileExt = null;
		for (File file : files) {
			if(file.isDirectory()){
			  continue;
			}
			String path = file.getPath();
			if(path.endsWith(".properties") || path.endsWith(".yaml") || path.endsWith(".yml")){
				if(path.contains("/i18n/"))continue;
				if(path.endsWith("pom.properties"))continue;
				fileExt = path.substring(path.lastIndexOf("."));
				if(!allFileMap.containsKey(fileExt)){
					allFileMap.put(fileExt, new ArrayList<String>());
				}
				allFileMap.get(fileExt).add(path);
			}
		}
		
		Set<String> fileExts = allFileMap.keySet();
		for (String key : fileExts) {
			parseConfigSortFiles(allFileMap.get(key), key, null);
		}
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
			System.out.println(">>load properties from file:" + fileList.get(0));
		}else if(fileList.size() > 1){
			Map<String, Properties> filePropMap = new LinkedHashMap<>(fileList.size());
			Properties p;
			for (String file : fileList) {
				filePropMap.put(file, p = parseToProperties(file, jarFile));
				if(profile == null && p.containsKey("spring.profiles.active")) {
					profile = replaceRefValue(p.getProperty("spring.profiles.active"));
				}
			}
			
			if(profile != null) {
				String suffix = "-" + profile + fileExt;
				for (String file : fileList) {
					if(file.endsWith(suffix)) {
						profileFile = file;
						profileProperties = filePropMap.get(file);
					}
				}
			}
			
			String fileName;
			for (String file : fileList) {
				if(file.contains("/")) {
					fileName = file.substring(file.lastIndexOf("/") + 1);
				}else {
					fileName = file.substring(file.lastIndexOf(File.separator) + 1);
				}
				if(fileName.startsWith("application-"))continue;
				if(fileName.startsWith("bootstrap-"))continue;
				allProperties.putAll(filePropMap.get(file));
				System.out.println(">>load properties from file:" + file);
			}
		}
	}
	
	private static Properties parseToProperties(String path,JarFile jarFile) throws FileNotFoundException, IOException{
		Properties properties = new Properties();
		Yaml yaml = null;
		if(path.endsWith(".yaml") || path.endsWith(".yml")){
			yaml = new Yaml();
		}
		if(jarFile == null){
			FileReader fileReader = new FileReader(path);
			if(yaml == null){
				properties.load(fileReader);
			}else{
				Map<String, Object> map = yaml.load(fileReader);
				parseYamlInnerMap(null, properties, map);
			}
			try {fileReader.close();} catch (Exception e) {}
		}else{
			InputStream inputStream = jarFile.getInputStream(jarFile.getJarEntry(path));
			if(yaml == null){
				properties.load(inputStream);
			}else{
				Map<String, Object> map = yaml.load(inputStream);
				parseYamlInnerMap(null, properties, map);
			}
			try {inputStream.close();} catch (Exception e) {}
		}
		return properties;
	}

	/**
	 * 获取所有配置的副本
	 * @return
	 */
	public static Properties getAllProperties() {
		return getAllProperties(null);
	}
	
	/**
	 * 按前缀匹配配置列表
	 * @param prefix
	 * @return
	 */
	public static Properties getAllProperties(String prefix) {
		return getAllProperties(prefix, true);
	}
	/**
	 * 按key模糊匹配配置列表
	 */
	public static Properties getAllProperties(String keyPattern,boolean matchPrefix) {
		
		Properties properties = new Properties();
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		boolean match = false;
		for (Entry<Object, Object> entry : entrySet) {
			match = StringUtils.isBlank(keyPattern) ;
			if(!match){
				if(matchPrefix){
					match = entry.getKey().toString().startsWith(keyPattern);
				}else{
					match = entry.getKey().toString().matches(keyPattern);
				}
			}
			if(match){	
				String value = replaceRefValue(entry.getValue().toString());
				properties.put(entry.getKey(), value);
			}
		}
		return properties;
	}
	
	public static String getProperty(String key) {
		return getProperty(key, null);
	}
	
	public static String getAnyProperty(String...keys) {
		String value;
		for (String key : keys) {
			value = getProperty(key);
			if(value != null)return value;
		}
		return null;
	}
	
	public static String getAndValidateProperty(String key) {
		String value = getProperty(key, null);
		if(StringUtils.isBlank(value)){
			throw new IllegalArgumentException(String.format("Property for key:%s not exists", key));
		}
		return value;
	}

	public static String getProperty(String key, String defaultValue) {
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
		String value = getProperty(key);
		return value != null ? Boolean.parseBoolean(value) : defalutValue;
	}
	
	public static List<String> getList(String key){
		String value = getProperty(key);
		if(StringUtils.isBlank(value))return new ArrayList<>(0);
		StringTokenizer st = new StringTokenizer(value, CONFIG_DELIMITERS);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if(StringUtils.isNotBlank(token)) {
				tokens.add(token);
			}
		}
		return tokens;
	}
	
	public static Map<String, String> getMappingValues(String prefix){
		Properties properties = getAllProperties(prefix);
		Map<String, String> result = new HashMap<>(properties.size());
		Set<Entry<Object, Object>> entries = properties.entrySet();
		for (Entry<Object, Object> entry : entries) {
			if(entry.getValue() == null || StringUtils.isBlank(entry.getValue().toString()))continue;
			String[] arr = StringUtils.split(entry.getKey().toString(), "[]");
			result.put(arr[1], entry.getValue().toString());
		}
		return result;
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
		return allProperties.containsKey(key);
	}
	
	public static boolean  containsAnyProperty(String...keys){
		for (String key : keys) {
			if(allProperties.containsKey(key))return true;
		}
		return false;
	}
	
	public static List<String>  getPropertyNames(String prefix){
		List<String> result=new ArrayList<>();
		Enumeration<Object> keys = allProperties.keys();
		while (keys.hasMoreElements()) {
			Object r = keys.nextElement();
			if (r.toString().startsWith(prefix)) {
				result.add(r.toString());
			}
		}
		return result;

	}
	
	public static <T> T getBean(String prefix,Class<T> clazz) {
		try {
			T config = clazz.newInstance();
			Field[] fields = clazz.getDeclaredFields();
			String configKey;
			Object configValue;
			for (Field field : fields) {
				field.setAccessible(true);
				configKey = prefix + field.getName();
				if(containsProperty(configKey)){
					if(field.getType() == int.class || field.getType() == Integer.class){
						configValue = Integer.parseInt(getProperty(configKey));
					}else if(field.getType() == long.class || field.getType() == Long.class){
						configValue = Long.parseLong(getProperty(configKey));
					}else if(field.getType() == boolean.class || field.getType() == Boolean.class){
						configValue = Boolean.parseBoolean(getProperty(configKey));
					}else{
						configValue = getProperty(configKey);
					}
					try {field.set(config, configValue);} catch (Exception e) {}
				}
			}
			return config;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
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
    
    public static void printAllConfigs() {
    	List<String> sortKeys = new ArrayList<>();
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			sortKeys.add(key);
		}
		Collections.sort(sortKeys);
		System.out.println("==================final config list start==================");
		String value;
		for (String key : sortKeys) {
			value = SafeStringUtils.hideSensitiveKeyValue(key, getProperty(key));
			System.out.println(String.format("%s = %s", key,value ));
		}
		System.out.println("==================final config list end====================");
    }
    
    private static String replaceRefValue(String value){
    	return replaceRefValue(allProperties, value);
    }
    
    private static void parseYamlInnerMap(String keyPrefix,Properties result,Map<String, Object> yamlData){
    	if(yamlData == null)return ;
		Object value;
		String currentKey;
		for (Object key : yamlData.keySet()) {
			currentKey = keyPrefix == null ? key.toString() : keyPrefix + "." + key.toString();
			value = yamlData.get(key);
			if(value == null)continue;
			if(value instanceof Map){
				parseYamlInnerMap(currentKey, result, (Map)value);
			}else{
				result.put(currentKey, value);
			}
		}
		
	}
    
    
}
