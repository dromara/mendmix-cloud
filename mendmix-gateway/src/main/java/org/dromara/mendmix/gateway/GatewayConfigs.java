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
package org.dromara.mendmix.gateway;

import java.util.List;

import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.springweb.AppConfigs;

/**
 * 
 * <br>
 * Class Name   : AppConfigs
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class GatewayConfigs extends AppConfigs {
	
	public static final String CORS_ENABLED_CONFIG_KEY = "mendmix-cloud.request.cors.enabled";
	
	public static final String OPENAPI_CLIENT_MAPPING_CONFIG_KEY = "mendmix-cloud.openapi.client-config.mapping";
	
	public static final String PATH_PREFIX = ResourceUtils.getProperty("mendmix-cloud.request.pathPrefix", "/api");

	public static final String GLOBAL_ANON_URI_CONFIG_KEY = "mendmix-cloud.acl.global.anonymousUris";
	
public static final List<String> anonymousIpWhilelist = ResourceUtils.getList("mendmix-cloud.acl.anonymous-ip-whilelist");
	
	public static final boolean openApiEnabled = ResourceUtils.getBoolean("mendmix-cloud.openapi.enabled", false);
	public static final boolean openApiScopeEnabled = ResourceUtils.getBoolean("mendmix-cloud.openapi.apiscope.enabled", false);
	
	public static final long REQUEST_TIME_OFFSET_THRESHOLD = ResourceUtils.getLong("mendmix-cloud.timestamp.offset.threshold.ms", 60000 * 5);
	public static final boolean WEB_REQUEST_SIGN_ENABLED = ResourceUtils.getBoolean("mendmix-cloud.webrequest.sign.enabled");
	
	
	public static final List<String> ignoreRewriteRoutes = ResourceUtils.getList("mendmix-cloud.response.rewrite.ignore-routes");
	
}
