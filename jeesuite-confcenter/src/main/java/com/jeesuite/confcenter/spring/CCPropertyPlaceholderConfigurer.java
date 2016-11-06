/**
 * 
 */
package com.jeesuite.confcenter.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import com.jeesuite.confcenter.utils.HttpUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class CCPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer{
	
	private final static Logger logger = LoggerFactory.getLogger(CCPropertyPlaceholderConfigurer.class);

	private boolean remoteEnabled = true;
	
	public void setRemoteEnabled(boolean remoteEnabled) {
		this.remoteEnabled = remoteEnabled;
	}

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {
		super.processProperties(beanFactoryToProcess, props);
	}



	@Override
	protected Properties mergeProperties() throws IOException {
		Properties properties = super.mergeProperties();
		if(remoteEnabled){			
			properties.putAll(loadRemoteProperties());
		}
		return properties;
		//
	}

	private Map<String, Object> loadRemoteProperties() throws FileNotFoundException, IOException{
		Map<String, Object> props = new HashMap<>();
		List<File> files = fecthRemoteConfigFile();
		for (File file : files) {
			Properties p = new Properties();
			p.load(new FileReader(file));
			
			for(String key : p.stringPropertyNames()) { 
				String value = p.getProperty(key);
				if(value != null && !"".equals(value.toString().trim())){
					props.put(key, value);
				}
			}
		}
		return props;
	}
	
	private List<File> fecthRemoteConfigFile() throws FileNotFoundException, IOException{
		List<File> files = new ArrayList<>(); 
		String classRootDir = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		if(!classRootDir.endsWith("/"))classRootDir = classRootDir + "/";
		
		List<String> urls = parseAllRemoteUrls();
		for (String url : urls) {
			String fileName = url.split("file=")[1];
			logger.info("begin download remote file by url:{}",url);
			File file = HttpUtils.downloadFile(url, classRootDir + "config.properties");
			if(file != null){				
				files.add(file);
				logger.info("download {} ok!!",file.getPath());
			}else{
				logger.warn("download file[{}] failture from:{}",fileName,url);
			}
		}
		
		return files;
	}
	
	private List<String> parseAllRemoteUrls() throws FileNotFoundException, IOException{
		
		List<String> result = new ArrayList<>();
		URL resource = Thread.currentThread().getContextClassLoader().getResource("confcenter.properties");
		if(resource == null)throw new FileNotFoundException("配置文件[confcenter.properties]缺失");
		Properties p = new Properties();
		p.load(new FileReader(new File(resource.getPath())));
		String apiUrl = p.getProperty("confcenter.url");
		String app = p.getProperty("app.name");
		String env = p.getProperty("app.env");
		String version = p.getProperty("app.version");
		
		String[] appFiles = StringUtils.commaDelimitedListToStringArray(p.getProperty("app.config.files"));
		for (String file : appFiles) {
			result.add(String.format("%s/%s?env=%s&ver=%s&file=%s", apiUrl,app,env,version,file));
		}
		
		return result;
	}
	
}
