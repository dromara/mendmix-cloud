/**
 * 
 */
package com.jeesuite.rest.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.jeesuite.rest.RequestHeader;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月23日
 */
public class I18nUtils {
	
	private static Logger logger = LoggerFactory.getLogger(I18nUtils.class);

	private static ResourceBundleMessageSource messageSource;
	
	private static String classPath;
	
	static{
		List<String> i18nBaseNames = new ArrayList<>();
		loadResourceBaseNames(i18nBaseNames, "gloal_i18n/");//全局的
		loadResourceBaseNames(i18nBaseNames, "i18n/");//项目自身的
		if(!i18nBaseNames.isEmpty()){
			messageSource = new ResourceBundleMessageSource();
			messageSource.setBasenames(i18nBaseNames.toArray(new String[0]));
			logger.info("messageSource baseNames:{}",i18nBaseNames);
		}
	}

	/**
	 * @param i18nBaseNames
	 * @param dirName
	 */
	private static void loadResourceBaseNames(List<String> i18nBaseNames, String dirName) {
		URL resource = Thread.currentThread().getContextClassLoader().getResource(dirName);
		if (resource != null) {			
			if (resource.getProtocol().equals("file")) {
				File i18nBaseFile = new File(resource.getPath());
				if(classPath == null){					
					classPath = i18nBaseFile.getParent();
					if(!classPath.endsWith("/"))classPath = classPath + "/";
				}
				
				parseResBaseName(i18nBaseFile, i18nBaseNames);
			} else if (resource.getProtocol().equals("jar")) {
				try {					
					String jarFilePath = resource.getFile();						
					jarFilePath	= jarFilePath.substring("file:".length(), jarFilePath.length() - "!/i18n/".length());
					jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
					
					JarFile jarFile = new JarFile(jarFilePath);
					
					Enumeration<JarEntry> entries = jarFile.entries(); 
			        while (entries.hasMoreElements()) {  
			        	JarEntry entry = entries.nextElement();
			        	if(entry.getName().endsWith(".properties") && entry.getName().contains("i18n/")){
			        		String parentPath = entry.getName().substring(0,entry.getName().lastIndexOf("/")+1);
			        		String fileName = entry.getName().substring(entry.getName().lastIndexOf("/")+1);
			        		String baseName = parentPath + fileName.substring(0, fileName.length() - "_en_US.properties".length());
							if(!i18nBaseNames.contains(baseName)){
								i18nBaseNames.add(baseName);
							}
			        	}
			 
			        } 
			        jarFile.close();
				} catch (Exception e) {
					logger.error("load i18n properties error",e);
				}
			}
		}
	}
	
	private static void parseResBaseName(File parent, List<String> names){
		File[] files = parent.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				parseResBaseName(file, names);
			}else{
				String path = file.getPath();
				if(path.endsWith("properties")){
					String parentPath = file.getParent();
					if(!parentPath.endsWith("/"))parentPath = parentPath + "/";
					String resName = parentPath.replace(classPath, "") + file.getName().split("_")[0];
					if(!names.contains(resName)){
						names.add(resName);
					}
				}
			}
		}
	}
	
	public static String getMessage(RequestHeader header,String code,String defaultMessage){
		return getMessage(code, new Object[0],getLocale(header), defaultMessage);
	}
	
	public static String getMessage(RequestHeader header,String code,Object[] params,String defaultMessage){
		return getMessage(code, params,getLocale(header), defaultMessage);
	}
	
	public static String getMessage(String code,Locale locale,String defaultMessage){
		return getMessage(code, new Object[0],locale, defaultMessage);
	}
	
	public static String getMessage(String code,Object[] params,Locale locale,String defaultMessage){
		if(messageSource == null)return defaultMessage;
		try {	
			String message = messageSource.getMessage(code, params, locale);
			return message;
		} catch (Exception e) {
			return defaultMessage;
		}
	}
	
	private static Locale getLocale(RequestHeader header){
		Locale locale = Locale.CHINA;
//		try {
//			locale = StringUtils.isBlank(header.getLanguage()) ? Locale.CHINA : new Locale(header.getLanguage(),header.getArea());
//		} catch (Exception e) {
//			locale = Locale.CHINA;
//		}
		return locale;
	}
	
	public static void main(String[] args) {
		String base = "/datas/project/support/";
		String path = "/datas/project/support/i18n/aa/";
		System.out.println(path.replace(base, ""));
	}
}
