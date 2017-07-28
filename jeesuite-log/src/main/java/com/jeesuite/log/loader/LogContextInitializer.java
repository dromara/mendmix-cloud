package com.jeesuite.log.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
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

import com.jeesuite.common.util.ResourceUtils;

public class LogContextInitializer{

	
	
	public static void initialize() {
		initLog4j2WithoutConfigFile();
		System.setProperty("log:server", getIpAddress());
	} 
	
	/**
	 * 初始化日志配置
	 */
	public static void initLog4j2WithoutConfigFile() {
		System.out.println("no local log4j2.xml file found,init logContext");
		
		ConfigurationBuilder< BuiltConfiguration > builder =
		        ConfigurationBuilderFactory.newConfigurationBuilder();

		builder.setStatusLevel( Level.ERROR);
		builder.setConfigurationName("RollingBuilder");
		// create the console appender
		AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
		        ConsoleAppender.Target.SYSTEM_OUT);
		appenderBuilder.add(builder.newLayout("PatternLayout").
		        addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
		builder.add( appenderBuilder );

//		LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
//		        .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
//		ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
//		        .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
//		        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
//		appenderBuilder = builder.newAppender("rolling", "RollingFile")
//		        .addAttribute("fileName", "target/rolling.log")
//		        .addAttribute("filePattern", "target/archive/rolling-%d{MM-dd-yy}.log.gz")
//		        .add(layoutBuilder)
//		        .addComponent(triggeringPolicy);
//		builder.add(appenderBuilder);

		builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef("Stdout")));
		builder.add(builder.newLogger("com.jeesuite", Level.TRACE).add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
		 
		Configurator.initialize(builder.build());
	}
	
	public static void reload(){
		System.setProperty("appId", ResourceUtils.getProperty("log.appid"));
		System.setProperty("log.service.level", ResourceUtils.getProperty("log.service.level","INFO"));
		System.setProperty("log.framework.level", ResourceUtils.getProperty("log.framework.level","INFO"));
		String logDir = ResourceUtils.getProperty("log.output.dir","logs/");
		if(!logDir.endsWith("/"))logDir = logDir + "/";
		System.setProperty("log.output.dir", logDir);
		
		//MainMapLookup.setMainArguments("appId",ResourceUtils.getProperty("log.appid"));
		//
		reloadLog4j2WithXmlConfigFile();
	}
	
	@SuppressWarnings("rawtypes")
	private static String getIpAddress()  {  
		String ipAddr = null;
		try {			
			Enumeration en = NetworkInterface.getNetworkInterfaces();  
			outter:while (en.hasMoreElements()) {  
				NetworkInterface i = (NetworkInterface) en.nextElement();  
				for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {  
					InetAddress addr = (InetAddress) en2.nextElement();  
					if (!addr.isLoopbackAddress()) {  
						if (addr instanceof Inet4Address) {    
							ipAddr = addr.getHostAddress();  
							break outter;
						}  
					}  
				}  
			}  
		} catch (Exception e) {}
	    return ipAddr;  
	} 
	
	private static JarFile jarFile;
	/**
	 * 通过xml配置文件重新加载log4j2
	 * @throws IOException
	 */
	private static void reloadLog4j2WithXmlConfigFile() {
		
		if(!Boolean.parseBoolean(System.getProperty("log.reload","true"))){
			System.out.println("log.reload:false");
			return;
		}
		String outtype = ResourceUtils.getProperty("log.outtype", "kafka");
		System.out.println("outtype:"+outtype);
		if(StringUtils.equalsIgnoreCase("console", outtype))return;
		
		try {			
			// read log4j configuration from xml
			String log4jConfFileName = "log4j2-"+outtype+".xml";
			InputStream in = loadFileInputStream(log4jConfFileName);
			if(in == null){
				System.err.println("not found log4j configure file["+log4jConfFileName+"] in classpath");
				return ;
			}
			System.out.println("reload log4j from config:"+ log4jConfFileName);
			XmlConfigurationFactory factory = new XmlConfigurationFactory();
			
			
			// init context
			LoggerContext logContext = (LoggerContext) LogManager.getContext(true);
			Configuration configuration = factory.getConfiguration(logContext,new ConfigurationSource(in));
			logContext.stop();
			while(logContext.isStopping()){}
			
			if(logContext.isStopped()){	 
				logContext = (LoggerContext) LogManager.getContext(false);
				logContext.start(configuration);
				System.out.println("LoggerContext reload...");
				//logContext.reconfigure();
				StatusLogger.getLogger().reset();
			}
			while(logContext.isStarting()){}
			if(logContext.isStarted())System.out.println("LoggerContext is started!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {jarFile.close();} catch (Exception e) {}
	}
	
	private static InputStream loadFileInputStream(String fileName){
		URL url = Thread.currentThread().getContextClassLoader().getResource("");
		try {
			if (url.getProtocol().equals("file")) {				
				return new FileInputStream(new File(url.getPath(),fileName));
			}else if (url.getProtocol().equals("jar")) {
				String jarFilePath = url.getFile();	
				jarFilePath = jarFilePath.split("jar!")[0] + "jar";
				jarFilePath = jarFilePath.substring("file:".length());
				jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");

				jarFile = new JarFile(jarFilePath);
				Enumeration<JarEntry> entries = jarFile.entries(); 
				InputStream inputStream = null;
		        while (entries.hasMoreElements()) {  
		        	JarEntry entry = entries.nextElement();
		        	if(entry.getName().endsWith(fileName)){
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
	
	
//	private static String[] scanMapperPackages(){
//		
//		
//    	String classPattern = "/**/*.class";
//    	String scanBasePackage = "com.ayg";
//    	ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
//
//		try {
//            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(scanBasePackage)
//                    + classPattern;
//            org.springframework.core.io.Resource[] resources = resourcePatternResolver.getResources(pattern);
//            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
//            for (org.springframework.core.io.Resource resource : resources) {
//                if (resource.isReadable()) {
//                    MetadataReader reader = readerFactory.getMetadataReader(resource);
//                    String className = reader.getClassMetadata().getClassName();
//                    Class<?> clazz = Class.forName(className);
//                    if(clazz.isAnnotationPresent(MapperScan.class)){
//                    	String[] mapperPackages = clazz.getDeclaredAnnotation(MapperScan.class).basePackages();
//                    	for (String mapperPackage : mapperPackages) {
//							System.out.println("\n\n\n======mapperPackage========\n"+mapperPackage);
//						}
//                    	return mapperPackages;
//                    }
//                }
//            }
//        } catch (Exception e) {}
//	
//    	return new String[0];
//	}  
}
