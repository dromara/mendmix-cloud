package com.jeesuite.common.util;

/**
 * 这个一个上报工具：仅仅是为了知道谁在用我们
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @website <a href="http://www.jeesuite.com">vakin</a>
 * @date 2019年6月18日
 */
public class WhoUseMeReporter {

	private static volatile boolean done = false;
	
	/**
	 * 
	 * @param domain 域名
	 * @param product 我们的产品名称
	 */
	public static void post(String domain,String product){
		if(done)return;
		done = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String url = String.format("http://www.jeesuite.com/whoami?domain=%s&product=%s", domain,product);
					HttpUtils.get(url);
				} catch (Exception e) {}
			}
		}).start();
	}
}
