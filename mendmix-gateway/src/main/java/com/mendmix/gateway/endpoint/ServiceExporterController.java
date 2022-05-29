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
package com.mendmix.gateway.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.endpoint.management.HandleParam;
import com.mendmix.gateway.endpoint.management.MgtHandler;
import com.mendmix.gateway.endpoint.management.ModuleMgtHandler;
import com.mendmix.gateway.endpoint.management.RouteMgtHandler;
import com.mendmix.gateway.endpoint.management.ScheduleMgtHandler;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
@RestController
@RequestMapping(GatewayConstants.PATH_PREFIX + "/serviceExporter")
public class ServiceExporterController {
	
	private static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");
	
	private static Map<String, MgtHandler> handlers = new HashMap<>();
	
	static {
		handlers.put("module", new ModuleMgtHandler());
		handlers.put("route", new RouteMgtHandler());
		handlers.put("schedule", new ScheduleMgtHandler());
	}
	
	@RequestMapping(value = "/{handleName}/{act}",method = {RequestMethod.GET,RequestMethod.POST})
	public WrapperResponse<?> process(ServerHttpRequest request,@PathVariable("handleName") String handleName,@PathVariable("act") String act) {
		WrapperResponse<?> respData = null;
		try {
	
			HandleParam handleParam = new HandleParam(request);
	
			if(!handlers.containsKey(handleName)) {
				throw new MendmixBaseException("NOT SUPPORT");
			}
			
			Object result = handlers.get(handleName).handleRequest(act, handleParam);
			respData = new WrapperResponse<>(result);
		} catch (Exception e) {
			logger.error(handleName+"_error",e);
			respData = WrapperResponse.fail(e);
		}
		
		return respData;
	}
}
