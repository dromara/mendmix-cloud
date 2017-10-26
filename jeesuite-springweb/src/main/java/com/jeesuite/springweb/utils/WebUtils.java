package com.jeesuite.springweb.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.springweb.WebConstants;

public class WebUtils {

	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	private static Pattern ipPattern = Pattern.compile("(\\d{1,3}\\.)+\\d{1,3}");
	
	public static boolean isAjax(HttpServletRequest request){
	    return  (request.getHeader(WebConstants.HEADER_REQUESTED_WITH) != null  
	    && XML_HTTP_REQUEST.equals(request.getHeader(WebConstants.HEADER_REQUESTED_WITH).toString())) ;
	}
	
	public static  void responseOutJson(HttpServletResponse response,String json) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("application/json; charset=utf-8");  
	    PrintWriter out = null;  
	    try {  
	        out = response.getWriter();  
	        out.append(json);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	}  
	
	public static  void responseOutHtml(HttpServletResponse response,String html) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("text/html; charset=utf-8");  
	    PrintWriter out = null;  
	    try {  
	        out = response.getWriter();  
	        out.append(html);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	}  
	
	public static  void responseOutJsonp(HttpServletResponse response,String callbackFunName,Object jsonObject) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("text/plain; charset=utf-8");  
	    PrintWriter out = null;  
	    
	    String json = (jsonObject instanceof String) ? jsonObject.toString() : JsonUtils.toJson(jsonObject);
	    String content = callbackFunName + "("+json+")";
	    try {  
	        out = response.getWriter();  
	        out.append(content);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	} 

	
	/**
	 * 是否通过api网关转发
	 * @param request
	 * @return
	 */
	public static  boolean isForwardRequest(HttpServletRequest request) {
		return StringUtils.isNotBlank(request.getHeader(WebConstants.HEADER_FORWARDED_HOST)) 
				&& StringUtils.isNotBlank(request.getHeader(WebConstants.HEADER_FORWARDED_PRIFIX));
	}
	
	/**
	 * 获取根域名
	 * @param request
	 * @return
	 */
	public static  String getRootDomain(HttpServletRequest request) {
		String host = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		if(StringUtils.isBlank(host))host = request.getServerName();
		
		if(ipPattern.matcher(host).matches() || IpUtils.LOCAL_HOST.equals(host)){
			return host;
		}
		
		String[] segs = StringUtils.split(host, ".");
		int len = segs.length;
		return segs[len - 2] + "."+ segs[len - 1];
	}
	
	public static  String getRootDomain(String url) {
		String host = getDomain(url);
		if(ipPattern.matcher(host).matches() || IpUtils.LOCAL_HOST.equals(host))return host;
		
		String[] segs = StringUtils.split(host, ".");
		int len = segs.length;
		return segs[len - 2] + "."+ segs[len - 1];
	}
	
	public static  String getDomain(String url) {
		String[] urlSegs = StringUtils.split(url,"/");
		return urlSegs[1];
	}
	
	public static  String getBaseUrl(String url) {
		String[] segs = StringUtils.split(url,"/");
		return segs[0] + "//" + segs[1];
	}
	
	public static String getBaseUrl(HttpServletRequest request){
        String baseUrl = null;			
		String proto = request.getHeader(WebConstants.HEADER_FORWARDED_ORIGN_PROTO);
		if(proto == null)proto = request.getHeader(WebConstants.HEADER_FORWARDED_PROTO);
		String host = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		String prefix = request.getHeader(WebConstants.HEADER_FORWARDED_PRIFIX);
		if(StringUtils.isAnyBlank(proto,host,prefix)){
			String[] segs = StringUtils.split(request.getRequestURL().toString(),"/");
			baseUrl = segs[0] + "//" + segs[1];
		}else{
			baseUrl = proto + "://" + host + prefix;
		}
		
		//由于nginx 没有设置  proxy_set_header   X-Forwarded-Proto $scheme;
		//导致https通过nginx转发后，在api网关获取到的scheme为：http
		if(baseUrl.startsWith("http:")){
			baseUrl = baseUrl.replace("http", "https");
		}
		
		return baseUrl;
	}
}
