/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.common;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.constants.StandardEnv;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.springframework.util.ClassUtils;

public class GlobalContext {
	
	public static final  long STARTUP_TIME = System.currentTimeMillis();
	private static WorkerIdGenerator workerIdGenerator;
	
	private static volatile int workerId;
	
	private static final List<String> tenantIds = new ArrayList<>();

	public static final String SYSTEM_KEY;
	public static final String APPID;
	public static final String ENV;
	
	private static String contextPath;
	private static String nodeName;
	
	private static File appDataDir;
	
	private static boolean starting = true;
	
	private static boolean isGateway;
	
	static {
		isGateway = ClassUtils.isPresent("org.springframework.cloud.gateway.filter.GlobalFilter", null);
		String env = ResourceUtils.getAnyProperty("spring.profiles.active","mendmix-cloud.config.profile");
		ENV = StringUtils.isBlank(env) ? "local" : env;
		APPID = ResourceUtils.getProperty("spring.application.name","unknow-service");
		String[] strings = StringUtils.split(APPID, "-");
		if(ResourceUtils.containsProperty("mendmix-cloud.system.identifier")) {
			SYSTEM_KEY = ResourceUtils.getProperty("mendmix-cloud.system.identifier");
		}else {
			SYSTEM_KEY = strings[0];
		}
		//
		contextPath = ResourceUtils.getProperty("server.servlet.context-path","");
		if (StringUtils.isNotBlank(contextPath) && contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.length() - 1);
		}
		//
		System.setProperty("context.systemKey", GlobalContext.SYSTEM_KEY);
		System.setProperty("context.appId", GlobalContext.APPID);
		System.setProperty("context.env", GlobalContext.ENV);
	}
	
	public static void setWorkIdGenerator(WorkerIdGenerator workIdGenerator) {
		GlobalContext.workerIdGenerator = workIdGenerator;
	}
	
	public static boolean isGateway() {
		return isGateway;
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
	
	public static boolean isDevEnv() {
		return StandardEnv.local.name().equalsIgnoreCase(ENV) || StandardEnv.dev.name().equalsIgnoreCase(ENV) ; 
	}

	public static int getWorkerId() {
		if(workerId > 0)return workerId;
		if(workerIdGenerator != null) {
			workerId = workerIdGenerator.generate(getNodeName());
		}else {
			workerId = RandomUtils.nextInt(10, 99);
		}
		System.out.println(">>workId = "+workerId);
		return workerId;
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
	
	public static boolean isStarting() {
		return starting;
	}

	public static void startFinished() {
		GlobalContext.starting = false;
		getWorkerId();
	}

	public static File getAppDataDir() {
		if(appDataDir != null)return appDataDir;
		String dataDir = ResourceUtils.getProperty("mendmix-cloud.data.dir",System.getProperty("java.io.tmpdir"));
		try {
			File dir = new File(dataDir,APPID);
			boolean exists;
			if(!(exists = dir.exists())) {
				exists = dir.mkdirs();
			}
			if(exists) {
				appDataDir = dir;
				System.out.println(">>>>>> application.data.dir = " + dataDir);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return appDataDir;
	}
}
