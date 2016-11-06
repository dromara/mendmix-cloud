/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Page;

public abstract class BaseController extends Controller {
	private static final String DEFAULT_PARAM_OBJECT_NAME = "_root";
	private static final String METHOD_POST = "POST";
	private static final String DEFAULT_ERROR_MSG = "操作失败";
	private static final String DEFAULT_SUCCESS_MSG = "操作成功";
	/**
	 * 
	 */
	protected static String BASE_URI;
	protected static final String ADD = "add";
	protected static final String EDIT = "edit";
	protected static final String DEL = "del";
	protected static final String SAVE = "save";
	protected static final int DEFAULT_PAGE_SIZE = 10;

	/**
	 * 跳转到成功页面
	 * 
	 * @param message
	 * @param redirctUrl
	 */
	public void renderSuccessed(String message, String redirctUrl) {
		setAttr("message", message);
		setAttr("redirctUrl", redirctUrl);
		render("../public/successed.ftl");
	}

	/**
	 * 跳转到错误页面
	 * 
	 * @param message
	 * @param redirctUrl
	 */
	public void renderFailed(String message, String redirctUrl) {
		setAttr("message", message);
		setAttr("redirctUrl", redirctUrl);
		render("../public/failed.ftl");
	}

	/**
	 * ajax请求返回状态
	 * 
	 * @param status
	 *            1：成功，0失败
	 * @param msg
	 */
	public void ajaxReturn(int status, String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"status\":\"").append(status).append("\",\"msg\":\"")
				.append(msg.toString()).append("\"}");
		renderJson(sb.toString());
	}

	public void ajaxSuccess(String... tips) {
		String msg = DEFAULT_SUCCESS_MSG;
		if (tips != null && tips.length > 0 && tips[0] != null) {
			msg = tips[0];
		}
		ajaxReturn(1, msg);
	}

	public void ajaxError(String... error) {
		String msg = DEFAULT_ERROR_MSG;
		if (error != null && error.length > 0 && error[0] != null) {
			msg = error[0];
		}
		ajaxReturn(0, msg);
	}

	/**
	 * ajax请求返回状态
	 * 
	 * @param status
	 *            1：成功，0失败
	 * @param msg
	 */
	public void ajaxReturn(int status, String msg, Object data) {
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("status", status);
		obj.put("msg", msg);
		obj.put("data", data);
		renderJson(obj);
	}


	protected String getBaseUri() {
		if (BASE_URI == null) {
			HttpServletRequest request = getRequest();
			StringBuilder sb = new StringBuilder();
			sb.append(request.getScheme()).append("://")
					.append(request.getServerName());
			if (request.getServerPort() != 80) {
				sb.append(":").append(request.getServerPort());
			}
			sb.append(request.getContextPath()).toString();
			BASE_URI = sb.toString();
		}
		return BASE_URI;
	}

	/**
	 * 获取客户端IP
	 * 
	 * @methodName: getClientIp
	 * @description: TODO
	 * @author: Administrator
	 * @createDate: 2014年4月2日
	 * @return
	 */
	protected String getClientIp() {
		String ip = getRequest().getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = getRequest().getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = getRequest().getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = getRequest().getRemoteAddr();
		}

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = getRequest().getHeader("http_client_ip");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = getRequest().getHeader("HTTP_X_FORWARDED_FOR");
		}
		// 如果是多级代理，那么取第一个ip为客户ip
		if (ip != null && ip.indexOf(",") != -1) {
			ip = ip.substring(ip.lastIndexOf(",") + 1, ip.length()).trim();
		}
		return ip;
	}

	/**
	 * 是否ajax请求
	 * 
	 * @methodName: isAjax
	 * @description: TODO
	 * @author: vakinge
	 * @createDate: 2014年6月5日
	 * @return
	 */
	protected boolean isAjax() {
		String header = getRequest().getHeader("X-Requested-With");
		return "XMLHttpRequest".equalsIgnoreCase(header);
	}
	
	protected boolean isPost() {
		return METHOD_POST.equals(getRequest().getMethod());
	}

	protected int getPageNo() {
		return getParaToInt("pageNo", 1);
	}

	protected int getPageSize() {
		return getParaToInt("pageSize", DEFAULT_PAGE_SIZE);
	}


	/**
	 * 封装分页组件数据
	 * 
	 * @param pageObject
	 */
	protected void pageBuilder(Page<?> pageObject) {
		setAttr("datas", pageObject.getList() == null ? new ArrayList<>()
				: pageObject.getList());
		setAttr("pageNo", pageObject.getPageNumber());
		setAttr("pageSize", pageObject.getPageSize());
		setAttr("totalPage", pageObject.getTotalPage());
		setAttr("totalRow", pageObject.getTotalRow());

		Map<String, String> params = new HashMap<String, String>();
		HttpServletRequest request = getRequest();
		Enumeration<String> e = request.getParameterNames();
		if (e.hasMoreElements()) {
			while (e.hasMoreElements()) {
				String name = e.nextElement();
				String[] values = request.getParameterValues(name);
				if (values.length == 1) {
					params.put(name, values[0]);
				} else {
					for (int i = 0; i < values.length; i++) {
						params.put(name + "[" + i + "]", values[i]);
					}
				}
			}
		}
		if (!params.isEmpty()) {
			setAttr("queryParameters", params);
		}

		setAttr("queryURI", request.getRequestURI());
	}

	protected Map<String, Object> getParameters() {
		return parseObjectParameters(getRequest()).get(DEFAULT_PARAM_OBJECT_NAME);
	}

	/**
	 * 解析并封装对象参数请求（如：user.id）
	 * @return
	 */
	protected static Map<String, Map<String, Object>> parseObjectParameters(HttpServletRequest request) {
		
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		Enumeration<String> e = request.getParameterNames();

		if (e.hasMoreElements()) {
			while (e.hasMoreElements()) {
				String name = e.nextElement();
				String[] nameParts = name.indexOf(".") <=0 ? new String[]{DEFAULT_PARAM_OBJECT_NAME,name} : name.split("\\.");
				Map<String, Object> params = result.get(nameParts[0]);
				if(params == null){
					params = new HashMap<>();
					result.put(nameParts[0], params);
				}
				String[] values = request.getParameterValues(name);
				if (values.length == 1) {
					if (StringUtils.isNotBlank(values[0]))
						params.put(nameParts[1], values[0]);
				} else {
					params.put(nameParts[1], values);
				}
			}
		}
		
		//post 请求解析流
		if(request.getMethod().equals(METHOD_POST)){
			String content = null;
			try {
				content = IOUtils.toString(request.getInputStream(),StandardCharsets.UTF_8);
			} catch (Exception e1) {}
			if(StringUtils.isNotBlank(content)){
				//JSON
				if(content.startsWith("{")){
					//TODO
				}else{					
					String[] split = content.split("\\&");
					for (String s : split) {
						String[] split2 = s.split("=");
						if (split2.length == 2 && StringUtils.isNotBlank(split2[1])) {
							String[] nameParts = split2[0].split("\\.");
							if(nameParts.length != 2)continue;
							Map<String, Object> params = result.get(nameParts[0]);
							if(params == null){
								params = new HashMap<>();
								result.put(nameParts[0], params);
							}
							params.put(nameParts[1], split2[1]);
						}
					}
				}
				
			}
		}

		return result;
	}

}