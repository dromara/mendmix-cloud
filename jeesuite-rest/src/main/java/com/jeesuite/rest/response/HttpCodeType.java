package com.jeesuite.rest.response;

public interface HttpCodeType {

	/**
	 * 获取异常代码
	 * 
	 * @return
	 */
	public int getCode();

	/**
	 * 获取异常信息
	 * 
	 * @return
	 */
	public String getMsg();
}
