/**
 * 
 */
package com.jeesuite.confcenter.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class HttpUtils {

	/**
	 * 下载远程文件并保存到本地
	 * 
	 * @param remoteFilePath
	 *            远程文件路径
	 * @param localFilePath
	 *            本地文件路径
	 */
	public static File downloadFile(String remoteFilePath, String localFilePath) {
		URL urlfile = null;
		HttpURLConnection httpUrl = null;
		BufferedReader br = null;
		FileOutputStream out = null;
		File f = new File(localFilePath);
		try {
			urlfile = new URL(remoteFilePath);
			httpUrl = (HttpURLConnection) urlfile.openConnection();
			httpUrl.connect();
			
			br = new BufferedReader(new InputStreamReader(httpUrl.getInputStream(),"utf-8"));
            String line = null;
            StringBuilder result = new StringBuilder();
            while(null != (line=br.readLine())){
            	if(line.equals("not_found")){
            		throw new RuntimeException("文件不存在");
            	}
            	if(line.equals("download_error")){
            		throw new RuntimeException("下载错误");
            	}
            	result.append(line).append("\n");
            }
            out = new FileOutputStream(f);  
            out.write(result.toString().getBytes("utf-8"));  
			httpUrl.disconnect();
			return f;
		} catch (IOException e) {
			throw new RuntimeException("下载错误");
		} finally {
			try {
				if(br != null)br.close();
				if(out != null)out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
