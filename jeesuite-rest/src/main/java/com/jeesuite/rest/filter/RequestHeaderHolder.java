/**
 * 
 */
package com.jeesuite.rest.filter;

import com.jeesuite.rest.RequestHeader;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月19日
 * @Copyright (c) 2015, vakinge@github
 */
public class RequestHeaderHolder {

	private static ThreadLocal<RequestHeader> holder = new ThreadLocal<>();
	
	
	public static void set(RequestHeader rh){
		holder.set(rh);
	}
	
	public static void clear(){
		holder.remove();
	}
	
	public static RequestHeader get(){
		RequestHeader header = holder.get();
		if(header == null){
			header = new RequestHeader();
			holder.set(header);
		}
		return header;
	}
}
