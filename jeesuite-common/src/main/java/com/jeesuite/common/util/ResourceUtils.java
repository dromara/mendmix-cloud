/**
 * 
 */
package com.jeesuite.common.util;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 资源文件加载工具类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2013年2月25日
 */
public class ResourceUtils {

	static Map<String, String> cache = new HashMap<>();

	static List<Properties> properties = new ArrayList<Properties>();
	
	static boolean inited;

	private synchronized static void load() {
		try {
            if(!properties.isEmpty())return;
			File dir = new File(Thread.currentThread().getContextClassLoader().getResource("").getPath());

			File[] propFiles = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith("properties");
				}
			});

			for (File file : propFiles) {
				Properties p = new Properties();
				p.load(new FileReader(file));
				properties.add(p);
			}
			inited = true;
		} catch (Exception e) {
			inited = true;
			throw new RuntimeException(e);
		}
	}

	public static String get(String key, String... defaults) {
		if(!inited)load();
		if (cache.containsKey(key)) {
			return cache.get(key);
		}

		String value = null;
		for (Properties prop : properties) {
			value = prop.getProperty(key);
			if (value != null) {
				cache.put(key, value);
				return value;
			}
		}

		if (defaults != null && defaults.length > 0) {
			return defaults[0];
		} else {
			return System.getProperty(key);
		}

	}

}
