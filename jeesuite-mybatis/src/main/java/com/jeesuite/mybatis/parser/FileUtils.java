/**
 * 
 */
package com.jeesuite.mybatis.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年3月23日
 * @Copyright (c) 2015, jwww
 */
public class FileUtils {

	public static List<File> listFiles(List<File> results,File directory, final String extensions) {
		
		File[] subFiles = directory.listFiles();
		for (File file : subFiles) {
			if(file.isDirectory()){
				listFiles(results, file, extensions);
			}else if(file.getName().endsWith(extensions)){
				results.add(file);
			}
		}		
		return results;
	}
	
	public static List<String> listFiles(JarFile jarFile, String extensions) {
		if (jarFile == null || StringUtils.isEmpty(extensions))
			return null;
		
		List<String> files = new ArrayList<String>();
		
		Enumeration<JarEntry> entries = jarFile.entries(); 
        while (entries.hasMoreElements()) {  
        	JarEntry entry = entries.nextElement();
        	String name = entry.getName();
    		
    		if (name.endsWith(extensions)) {
    			files.add(name);
    		}
        } 
        
        return files;
	}
}
