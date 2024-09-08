package org.dromara.mendmix.logging.applog.appender;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.logging.LogConstants;
import org.dromara.mendmix.logging.LogKafkaClient;
import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * 
 * @author jiangwei
 * @version 1.0.0
 * @date 2022年12月14日
 */
@Plugin(name = "PlatfromAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class PlatfromAppender extends AbstractAppender {

	private static final String LOG_SEPARATOR = "|";
	private static String topicName = ResourceUtils.getProperty("mendmix-cloud.logging.applog.topicName","mendmix_topic_appLog");
	private static int maxMessageSizeLimit = 1024;

	private static LogKafkaClient logKafkaClient;
	
	public static LogKafkaClient getLogKafkaClient() {
		if(logKafkaClient != null)return logKafkaClient;
		synchronized (PlatfromAppender.class) {
			if(logKafkaClient != null)return logKafkaClient;
			logKafkaClient = InstanceFactory.getInstance(LogKafkaClient.class);
		}
		return logKafkaClient;
	}

	protected PlatfromAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			boolean ignoreExceptions, Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void append(LogEvent event) {
		if(getLogKafkaClient() == null) {
			if(!GlobalContext.isStarting()) {
				System.err.println("logKafkaClient not init!!!");
			}
			return;
		}
		byte[] bytes = getLayout().toByteArray(event);
		String msg = new String(bytes);
		//%d{UNIX_MILLIS}|%level|%thread|%file:%line|%msg
		String[] parts = StringUtils.split(msg, LOG_SEPARATOR, 5);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(LogConstants.TIMESTAMP, parts[0]);
		map.put(LogConstants.LEVEL, parts[1]);
		map.put(LogConstants.THREAD, parts[2]);
		map.put(LogConstants.FILE_LINE, parts[3]);
		
		String message = parts[4];
		if(message != null && message.length() > maxMessageSizeLimit) {
			message = message.substring(0, maxMessageSizeLimit);
		}
		map.put(LogConstants.MESSAGE, message);
		//
		appendContextParams(map);
		getLogKafkaClient().send(topicName, JsonUtils.toJson(map));
	}
	
	

	private void appendContextParams(Map<String, Object> map) {
		map.put(LogConstants.TRACE_ID, CurrentRuntimeContext.getRequestId());
		map.put(LogConstants.BASIC , LogConstants.SERVICE_INFO);
		Map<String, Object> context = new LinkedHashMap<>(10);
		String contextVal = CurrentRuntimeContext.getClientType();
		if(contextVal != null)context.put(LogConstants.CLIENT_TYPE, contextVal);
		contextVal = CurrentRuntimeContext.getSystemId();
		if(contextVal != null)context.put(LogConstants.SYSTEM_ID, contextVal);
		contextVal = CurrentRuntimeContext.getTenantId();
		if(contextVal != null)context.put(LogConstants.TENANT_ID, contextVal);
        //
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null){
			map.put(LogConstants.USER_ID,currentUser.getId());
			map.put(LogConstants.USER_NAME,currentUser.getName());
			map.put(LogConstants.SUBJECT_ID,currentUser.getPrincipalId());
		}
		map.put(LogConstants.CONTEXT, context);
	}
	
	@Override
	public boolean stop(final long timeout, final TimeUnit timeUnit) {
		setStopping();
		boolean stopped = super.stop(timeout, timeUnit, false);
		setStopped();
		return stopped;
	}

	@PluginFactory
	public static PlatfromAppender createAppender(
			@PluginAttribute(value = "name") String name,
			@PluginAttribute(value = "queueCapacity") String queueCapacity,
			@PluginElement("Filter") final Filter filter,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
		if (layout == null) {
			layout = new JsonLayout.Builder<>().setCompact(true).setLocationInfo(true).setStacktraceAsString(true).build();
		}
		Validate.notEmpty(name, "name 不能为空");
		return new PlatfromAppender(name, filter, layout, ignoreExceptions,new Property[0]);
	}

}
