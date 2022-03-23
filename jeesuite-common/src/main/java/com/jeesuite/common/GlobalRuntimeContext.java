package com.jeesuite.common;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.ResourceUtils;

public class GlobalRuntimeContext {
	
	private static WorkIdGenerator workIdGenerator;
	
	private static volatile int workId;
	
	private static final List<String> tenantIds = new ArrayList<>();

	public static final String SYSTEM_ID;
	public static final String MODULE_NAME;
	public static final String APPID;
	public static final String ENV;
	
	private static String contextPath;
	private static String nodeName;
	
	static {
		String env = ResourceUtils.getAnyProperty("spring.profiles.active","jeesuite.config.profile");
		ENV = StringUtils.isBlank(env) ? "local" : env;
		APPID = ResourceUtils.getProperty("spring.application.name","unknow-service");
		String[] strings = StringUtils.split(APPID, "-");
		MODULE_NAME = strings.length > 1 ? strings[1] : strings[0];
		if(ResourceUtils.containsProperty("system.id")) {
			SYSTEM_ID = ResourceUtils.getProperty("system.id");
		}else {
			SYSTEM_ID = strings[0];
		}
		//
		contextPath = ResourceUtils.getProperty("server.servlet.context-path","");
		if (StringUtils.isNotBlank(contextPath) && contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.length() - 1);
		}
		//
		System.getProperty("env", ENV);
	}
	
	public static void setWorkIdGenerator(WorkIdGenerator workIdGenerator) {
		GlobalRuntimeContext.workIdGenerator = workIdGenerator;
	}

	public static String getContextPath() {
		return contextPath;
	}
	
	public static void addTenantId(String tenantId){
		if(tenantIds.contains(tenantId))return;
		tenantIds.add(tenantId);
	}

	public static List<String> getTenantids() {
		return Collections.unmodifiableList(tenantIds);
	}
	

	public static int getWorkId() {
		if(workId > 0)return workId;
		if(workIdGenerator != null) {
			workId = workIdGenerator.generate(getNodeName());
		}else {
			workId = RandomUtils.nextInt(10, 99);
		}
		return workId;
	}

	public static String getNodeName() {
		if(nodeName != null)return nodeName;
		try {
			nodeName = InetAddress.getLocalHost().getHostAddress() + ":" + ResourceUtils.getProperty("server.port", "8080");
		} catch (Exception e) {
			nodeName = "127.0.0.1:" + ResourceUtils.getProperty("server.port", "8080");
		}
		return nodeName;
	}
}
