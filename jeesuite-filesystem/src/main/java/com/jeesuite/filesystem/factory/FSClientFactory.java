/**
 * 
 */
package com.jeesuite.filesystem.factory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.Validate;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.provider.fdfs.FdfsProvider;
import com.jeesuite.filesystem.provider.qiniu.QiniuProvider;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class FSClientFactory {

	private FSClientFactory() {}

    private static AtomicBoolean inited = new AtomicBoolean(false);
	private static Map<String, FSProvider> fsProviders = new HashMap<String, FSProvider>();
	
	public static FSProvider build(String providerName,String group){
		
		String key = providerName + group;
		if(!inited.get()){
			synchronized (FSClientFactory.class) {
				if(fsProviders.isEmpty()){
					inited.set(true);
					String groupNames = ResourceUtils.getProperty("fs.groupNames");
					Validate.notBlank(groupNames, "[groupNames] not defined");
					String[] groups = groupNames.split(",|;");
					for (String g : groups) {
						String provider = ResourceUtils.getProperty("fs."+g+".provider");
		                   if(!QiniuProvider.NAME.equals(provider) && !FdfsProvider.NAME.equals(provider)){
		                	   throw new RuntimeException("Provider[" + provider + "] not support");
							}
						String accessKey = ResourceUtils.getProperty("fs."+g+".accessKey");
						String secretKey = ResourceUtils.getProperty("fs."+g+".secretKey");
						String urlprefix = ResourceUtils.getProperty("fs."+g+".urlprefix");
						Validate.notBlank(urlprefix, "[urlprefix] not defined");
						String servers = ResourceUtils.getProperty("fs."+g+".servers");
						String charset = ResourceUtils.getProperty("fs."+g+".charset");
						long connectTimeout = Long.parseLong(ResourceUtils.getProperty("fs."+g+".connectTimeout","3000"));
						int maxThreads = Integer.parseInt(ResourceUtils.getProperty("fs."+g+".maxThreads","50"));
						
						FSProvider fsProvider = null;
						if(QiniuProvider.NAME.equals(provider)){
							Validate.notBlank(accessKey, "[accessKey] not defined");
							Validate.notBlank(secretKey, "[secretKey] not defined");
							fsProvider = new QiniuProvider(urlprefix, g, accessKey, secretKey);
						}else if(FdfsProvider.NAME.equals(provider)){
							Validate.isTrue(servers != null && servers.matches("^.+[:]\\d{1,5}\\s*$"),"[servers] is not valid");
							String[] serversArray = servers.split(",|;");
							fsProvider = new FdfsProvider(urlprefix, g, serversArray, connectTimeout, maxThreads);
						}
						//
						fsProviders.put(provider + g, fsProvider);
					}
				}
			}
		}
		return fsProviders.get(key);
	}
	
	public void closeAll(){
		Collection<FSProvider> providers = fsProviders.values();
		for (FSProvider p : providers) {			
			try {p.close();} catch (Exception e) {}
		}
	}
	
}
