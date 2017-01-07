/**
 * 
 */
package com.jeesuite.filesystem.factory;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.provider.qiniu.QiniuProvider;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class DefaultFileClient {

	private DefaultFileClient() {}

	

	private static volatile FSProvider engine;
	
	public static FSProvider get(){
		if(engine == null){
			synchronized (DefaultFileClient.class) {
				if(engine == null){
					String bucketName = ResourceUtils.get("fs.bucketName");
					String accessKey = ResourceUtils.get("fs.accessKey");
					String secretKey = ResourceUtils.get("fs.secretKey");
					String urlprefix = ResourceUtils.get("fs.urlprefix");
					
					engine = new QiniuProvider(urlprefix, bucketName, accessKey, secretKey);
				}
			}
		}
		return engine;
	}
	
}
