package com.mendmix.gateway.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mendmix.common.annotation.ApiMetadata;
import com.mendmix.common.constants.PermissionLevel;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.gateway.model.WebConfig;

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
