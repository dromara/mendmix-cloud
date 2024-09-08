/**
 * 
 */
package org.dromara.mendmix.gateway.endpoint;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.annotation.ApiMetadata;
import org.dromara.mendmix.common.constants.PermissionLevel;

import io.netty.util.internal.PlatformDependent;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2024年7月12日
 */
@RestController
@RequestMapping("/exporter")
public class ExporterController extends org.dromara.mendmix.springweb.exporter.ExporterController implements InitializingBean{

	private long directMemoryLimit = 0;
	private long directMemorySafeThreshold = 0;
	
	@ApiMetadata(permissionLevel = PermissionLevel.Anonymous,actionLog = false)
	@GetMapping("/directMemoryStat")
	public Map<String, String> directMemoryStat() throws IllegalArgumentException, IllegalAccessException {
		Field counterField = ReflectionUtils.findField(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
		counterField.setAccessible(true);
		
		//
		AtomicLong usedMemory = (AtomicLong) counterField.get(PlatformDependent.class);
		
		Map<String, String> stats = new LinkedHashMap<String, String>(3);
		
		DataSize dataSize = DataSize.ofBytes(directMemoryLimit);
		stats.put("nodeName", GlobalContext.getNodeName());
		stats.put("limitMemory", dataSize.toMegabytes() + "MB");
		dataSize = DataSize.ofBytes(usedMemory.get());
		stats.put("usedMemory", dataSize.toString());
		if(dataSize.toMegabytes() > 0) {
			stats.put("formatUsedMemory", dataSize.toMegabytes() + "MB");
		}else if(dataSize.toKilobytes() > 0){
			stats.put("formatUsedMemory", dataSize.toKilobytes() + "KB");
		}
		
		return stats;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Field limitField = ReflectionUtils.findField(PlatformDependent.class, "DIRECT_MEMORY_LIMIT");
		limitField.setAccessible(true);
		directMemoryLimit = (long) limitField.get(PlatformDependent.class);
		directMemorySafeThreshold = directMemoryLimit / 10;
		long defaultThreshold = 8 * 1024 * 1024;
		if(directMemorySafeThreshold > defaultThreshold) {
			directMemorySafeThreshold = defaultThreshold;
		}
	}
}
