/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
	
	private static List<String> fileNames;
	private static List<String> fileExtensions = Arrays.asList(".properties",".yaml",".yml");
	
	public static String CONFIG_DELIMITERS = ",; \t\n";
	public static final String NULL_VALUE_PLACEHOLDER = "_NULL_PLACEHOLDER_";
	public static final String PLACEHOLDER_PREFIX = "${";
	public static final String PLACEHOLDER_SUFFIX = "}";

	private static String profile;
	private static String profileFile;
	private static Properties profileProperties;
	
	private final static Properties allProperties = new Properties();
	
	
	static {
		String propVal = System.getProperty("mendmix-cloud.configs", "application,bootstrap");
		fileNames = Arrays.asList(propVal.split(","));
		loadLocalConfigs();
	}
	
	private static void loadLocalConfigs() {
		try {
			profile = System.getProperty("spring.profiles.active");
			URL url = Thread.currentThread().getContextClassLoader().getResource("");
			if(url == null)url = ResourceUtils.class.getResource("");
			//指定外部配置
			String customLocation = System.getProperty("spring.config.location");
			if(StringUtils.isNotBlank(customLocation)) {
				System.out.println(String.format(">>using customLocation:%s", customLocation));
				Properties properties = new Properties();
				FileReader fileReader = new FileReader(customLocation);
				properties.load(fileReader);
				allProperties.putAll(properties);
				return;
			}
			Map<String, List<String>> allFileMap = new HashMap<>();
			if(url != null){
				if (url.getProtocol().equals("file")) {	
					String path = java.net.URLDecoder.decode(url.getPath(),"UTF-8");
					File parent = new File(path);
					if(!parent.exists()){
						System.err.println("<startup-logging> loadPropertiesFromFile_error,dir not found");
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
				System.out.println("<startup-logging>   load properties from file:" + profileFile);
			}
			//额外外部配置，覆盖本地配置
			String addtionalLocation = System.getProperty("spring.config.additional-location");
			if(StringUtils.isNotBlank(addtionalLocation)) {
				System.out.println(String.format(">>add addtional configs:%s", addtionalLocation));
				Properties properties = new Properties();
				FileReader fileReader = new FileReader(addtionalLocation);
				properties.load(fileReader);
				allProperties.putAll(properties);
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
			String fileName;
			if(entry.getName().contains("/")) {
				fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
			}else {
				fileName = entry.getName().substring(entry.getName().lastIndexOf(File.separator) + 1);
			}
			if(!fileNames.stream().anyMatch(prefix -> fileName.startsWith(prefix))) {
				continue;
			}
			if(!fileExtensions.stream().anyMatch(suffix -> fileName.endsWith(suffix))) {
				continue;
			}
			fileExt = entry.getName().substring(entry.getName().lastIndexOf("."));
			if(!allFileMap.containsKey(fileExt)){
				allFileMap.put(fileExt, new ArrayList<String>());
			}
			allFileMap.get(fileExt).add(entry.getName());
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
			if(!fileNames.stream().anyMatch(prefix -> file.getName().startsWith(prefix))) {
				continue;
			}
			if(!fileExtensions.stream().anyMatch(suffix -> file.getName().endsWith(suffix))) {
				continue;
			}
			String path = file.getPath();
			fileExt = path.substring(path.lastIndexOf("."));
			if(!allFileMap.containsKey(fileExt)){
				allFileMap.put(fileExt, new ArrayList<String>());
			}
			allFileMap.get(fileExt).add(path);
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
			System.out.println("<startup-logging>   load properties from file:" + fileList.get(0));
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
				//profile
				if(fileName.contains("-"))continue;				
				allProperties.putAll(filePropMap.get(file));
				System.out.println("<startup-logging>   load properties from file:" + file);
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
			if(StringUtils.isBlank(key))continue;
			value = System.getProperty(key);
			if(StringUtils.isNotBlank(value))return value;
		}
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
		if(StringUtils.isBlank(key))return null;
		//优先环境变量
		String value = System.getProperty(key);
		if(StringUtils.isNotBlank(value))return StringUtils.trimToNull(value);
		
		value = System.getenv(key);
		if(StringUtils.isNotBlank(value))return StringUtils.trimToNull(value);
		
		value = allProperties.getProperty(key);
		if (StringUtils.isNotBlank(value)) {
			value = replaceRefValue(value);
			return StringUtils.trimToNull(value);
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
		String key;
		for (Entry<Object, Object> entry : entries) {
			final String value = StringUtils.trimToNull(Objects.toString(entry.getValue(), null));
			if(value == null)continue;
			key = entry.getKey().toString();
			if(key.contains("[")) {
				String[] arr = StringUtils.split(key, "[]");
				result.put(arr[1], value);
			}else {
				int index = prefix.endsWith(".") ? prefix.length() : prefix.length() + 1;
				result.put(key.substring(index), value);
			}
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
		return System.getProperties().containsKey(key) || allProperties.containsKey(key);
	}
	
	public static boolean  containsAnyProperty(String...keys){
		for (String key : keys) {
			if(containsProperty(key))return true;
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
	
	@Deprecated
	public static <T> T getBean(String prefix,Class<T> clazz) {
		return getConfigObject(prefix, clazz);
	}
	
	public static <T> T getConfigObject(String prefix,Class<T> clazz) {
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
					}else if(field.getType() == double.class || field.getType() == Double.class){
						configValue = Double.parseDouble(getProperty(configKey));
					}else if(field.getType() == boolean.class || field.getType() == Boolean.class){
						configValue = Boolean.parseBoolean(getProperty(configKey));
					}else if(List.class.isAssignableFrom(field.getType())) {
						configValue = getList(configKey);
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
	
	public static <T> List<T> getConfigObjects(String prefix,Class<T> clazz) {
		List<T> configs = new ArrayList<>();
		int index = 0;
		while(true) {
			String withIndexPrefix = prefix + "[" + index + "].";
			if(!allProperties.keySet().stream().anyMatch(o -> o.toString().contains(withIndexPrefix))) {
				break;
			}
			configs.add(getConfigObject(withIndexPrefix, clazz));
			index++;
		}
		return configs;
	}
	
	public synchronized static void remove(String key){
		if(StringUtils.isBlank(key))return;
		allProperties.remove(key);
	}
	
	/**
	 * 如果替换包含占位符则替换占位符
	 * @param key
	 * @return
	 */
    private static String replaceRefValue(Properties properties,String value ) {
		
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
    
    public static void printAllConfigs(List<String> hiddenKeys) {
    	List<String> sortKeys = new ArrayList<>();
		Set<Entry<Object, Object>> entrySet = allProperties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			if(hiddenKeys != null && hiddenKeys.contains(key))continue;
			sortKeys.add(key);
		}
		Collections.sort(sortKeys);
		System.out.println(">>merge effectivec configs:");
		String value;
		for (String key : sortKeys) {
			value = getProperty(key);
			value = StringConverter.hideSensitiveKeyValue(key, value);
			System.out.println(String.format(" - %s = %s", key,value ));
		}
    }
    
    public static String replaceRefValue(String value){
    	return replaceRefValue(allProperties, value);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static void parseYamlInnerMap(String keyPrefix,Properties result,Map<String, Object> yamlData){
    	if(yamlData == null)return ;
		Object value;
		String currentKey;
		for (Object key : yamlData.keySet()) {
			currentKey = keyPrefix == null ? key.toString() : keyPrefix + "." + key.toString();
			value = yamlData.get(key);
			if(value == null || StringUtils.isBlank(value.toString()))continue;
			if(value instanceof List){
				int index = 0;
				for (Object subValue : (List)value) {
					if(index > 0) {
						currentKey = currentKey.substring(0,currentKey.lastIndexOf("["));
					}
					currentKey = currentKey + "["+index+"]";
					if(subValue instanceof Map){
						parseYamlInnerMap(currentKey, result, (Map)subValue);
					}else{
						result.setProperty(currentKey, subValue.toString());
					}
					index++;
				}
			}else if(value instanceof Map){
				parseYamlInnerMap(currentKey, result, (Map)value);
			}else{
				result.setProperty(currentKey, value.toString());
			}
		}
		
	}
    
}
