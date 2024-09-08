package org.dromara.mendmix.springweb.exporter;

import java.util.HashMap;
import java.util.Map;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.annotation.ApiMetadata;
import org.dromara.mendmix.common.constants.PermissionLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月26日
 */
@RestController
@RequestMapping("/exporter")
public class ExporterController {
	
	@ApiMetadata(permissionLevel = PermissionLevel.Anonymous,actionLog = false,responseKeep = true)
	@GetMapping("/info")
	public Map<String, Object> info(){
		Map<String, Object> result = new HashMap<>();
		result.put("env", GlobalContext.ENV);
		result.put("systemKey", GlobalContext.SYSTEM_KEY);
		result.put("serviceId", GlobalContext.APPID);
		result.put("workerId", GlobalContext.getWorkerId());
		result.put("startTime", GlobalContext.STARTUP_TIME);
		return result;
	}
	
}
