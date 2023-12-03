/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.logging.applog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;

import com.mendmix.common.util.IpUtils;
import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * 
 * <br>
 * Class Name : LogProfileManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年7月8日
 */
public class LogProfileManager {

	private static JarFile jarFile;

	public static void initialize() {
		InputStream in = loadFileInputStream("log4j2.xml");
		if (in != null) {
			try {
				jarFile.close();
			} catch (Exception e) {
			}
			return;
		}
		initLog4j2WithoutConfigFile();
		System.setProperty("logcontext-server", IpUtils.getLocalIpAddr());
	}

	/**
	 * 初始化日志配置
	 */
	private static void initLog4j2WithoutConfigFile() {
		System.out.println("no local log4j2.xml file found,init logContext");

		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		builder.setStatusLevel(Level.DEBUG);
		// builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT,
		// Filter.Result.NEUTRAL).
		// addAttribute("level", Level.INFO));
		AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
				ConsoleAppender.Target.SYSTEM_OUT);
		appenderBuilder.add(
				builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %c{2} %-5level: %msg%n%throwable"));
		appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
				.addAttribute("marker", "FLOW"));
		builder.add(appenderBuilder);
		builder.add(builder.newLogger("com.zy", Level.DEBUG).add(builder.newAppenderRef("Stdout"))
				.addAttribute("additivity", true));
		builder.add(builder.newLogger("com.mendmix", Level.TRACE).add(builder.newAppenderRef("Stdout"))
				.addAttribute("additivity", true));
		builder.add(builder.newLogger("com.zyframework", Level.DEBUG).add(builder.newAppenderRef("Stdout"))
				.addAttribute("additivity", true));
		builder.add(builder.newLogger("org.apache.ibatis", Level.DEBUG).add(builder.newAppenderRef("Stdout"))
				.addAttribute("additivity", true));
		builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef("Stdout")));

		Configurator.initialize(builder.build());
	}

	public static void reload() {

		Properties logProperties = ResourceUtils.getAllProperties("log.");
		logProperties.forEach((k, v) -> {
			if (v != null && k.toString().endsWith(".level")) {
				System.setProperty(k.toString(), v.toString());
			}
		});

		// POD_NAME:micro-staff-app-deploy-c7fb585c9-ck2v7
		// POD_IP:172.24.20.17
		String nodeName = System.getenv("POD_IP");
		if (StringUtils.isBlank(nodeName)) {
			nodeName = IpUtils.getLocalIpAddr();
		}
		System.setProperty("nodeName", nodeName);

		String logBasePath = ResourceUtils.getProperty("log.output.dir");
		if (StringUtils.isBlank(logBasePath)) {
			logBasePath = "./logs";
		}

		if (logBasePath.endsWith("/"))
			logBasePath = logBasePath.substring(0, logBasePath.length() - 1);
		System.setProperty("log.output.dir", logBasePath);

		reloadLog4j2WithXmlConfigFile();
	}

	/**
	 * 通过xml配置文件重新加载log4j2
	 * 
	 * @throws IOException
	 */
	private static void reloadLog4j2WithXmlConfigFile() {
		LoggerContext logContext = (LoggerContext) LogManager.getContext(false);

		String profile = ResourceUtils.getProperty("log.profile", "default");
		//
		System.out.println("log config");
		System.out.println(" -log.profile          =    " + profile);
		if ("file".equals(profile))
			System.out.println(" -log.output.dir =    " + System.getProperty("log.output.dir"));
		Properties properties = ResourceUtils.getAllProperties("log.");
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		boolean withLogLevelItem = false;
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			if (!key.endsWith(".level")) {
				continue;
			}
			withLogLevelItem = true;
			System.setProperty(key, entry.getValue().toString());
			System.out.println(key + "   =   " + entry.getValue());
		}

		if ("default".equals(profile)) {
			if (withLogLevelItem) {
				logContext.reconfigure();
				System.out.println("LoggerContext[" + logContext.getName() + "] reconfigured!");
			}
			return;
		}

		try {
			String log4jConfFileName = "log4j2-" + profile + ".xml";
			InputStream in = loadFileInputStream(log4jConfFileName);
			if (in == null) {
				System.err.println("not found log4j configure file[" + log4jConfFileName + "] in classpath");
				return;
			}
			System.out.println("reload log4j from config:" + log4jConfFileName);

			LogManager.shutdown(true, true);
			while (logContext.isStopping()) {
			}

			XmlConfigurationFactory factory = new XmlConfigurationFactory();
			Configuration configuration = null;
			logContext = (LoggerContext) LogManager.getContext(false);
			configuration = factory.getConfiguration(logContext, new ConfigurationSource(in));

			URI uri = Thread.currentThread().getContextClassLoader().getResource(log4jConfFileName).toURI();
			logContext.setConfigLocation(uri);
			StatusLogger.getLogger().reset();
			logContext.start(configuration);
			System.out.println("LoggerContext[" + logContext.getName() + "] reload...");

			while (logContext.isStarting()) {
			}
			if (logContext.isStarted())
				System.out.println("LoggerContext[" + logContext.getName() + "] is started!");
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			jarFile.close();
		} catch (Exception e) {
		}
	}

	private static InputStream loadFileInputStream(String fileName) {
		URL url = Thread.currentThread().getContextClassLoader().getResource("");
		try {
			if (url.getProtocol().equals("file")) {
				File logConfigFile = new File(url.getPath(), fileName);
				if (!logConfigFile.exists())
					return null;
				return new FileInputStream(logConfigFile);
			} else if (url.getProtocol().equals("jar")) {
				String jarFilePath = url.getFile();
				if (jarFilePath.contains("war!")) {
					jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "war!")[0] + "war";
				} else if (jarFilePath.contains("jar!")) {
					jarFilePath = StringUtils.splitByWholeSeparator(jarFilePath, "jar!")[0] + "jar";
				}
				jarFilePath = jarFilePath.substring("file:".length());
				jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");

				jarFile = new JarFile(jarFilePath);
				Enumeration<JarEntry> entries = jarFile.entries();
				InputStream inputStream = null;
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(fileName)) {
						inputStream = jarFile.getInputStream(jarFile.getJarEntry(entry.getName()));
						break;
					}
				}
				return inputStream;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
