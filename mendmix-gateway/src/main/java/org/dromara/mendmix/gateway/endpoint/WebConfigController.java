package org.dromara.mendmix.gateway.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.dromara.mendmix.common.annotation.ApiMetadata;
import org.dromara.mendmix.common.constants.PermissionLevel;
import org.dromara.mendmix.common.model.WrapperResponse;
import org.dromara.mendmix.gateway.model.WebConfig;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月26日
 */
@RestController
public class WebConfigController {

	@ApiMetadata(permissionLevel = PermissionLevel.Anonymous,actionLog = false)
	@GetMapping("/webconfig")
	@ResponseBody
	public WrapperResponse<WebConfig> webConfig(){
		WebConfig webConfig = WebConfig.getDegault();
		return WrapperResponse.success(webConfig); 
	}
}
