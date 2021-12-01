package com.jeesuite.springweb.utils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.util.SimpleCryptUtils;


/**
 * 自定义请求header读取工具
 * <br>
 * Class Name   : CustomHeaderUtils
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021年3月20日
 */
public class CustomHeaderUtils {

	/**
	 * 获取当前请求已验证手机
	 * @return
	 */
	public static String getVerifiedMobile() {
		String header = CurrentRuntimeContext.getRequest().getHeader(CustomRequestHeaders.HEADER_VERIFIED_MOBILE);
		if(header == null)return header;
		return SimpleCryptUtils.decrypt(header);
	}
}
