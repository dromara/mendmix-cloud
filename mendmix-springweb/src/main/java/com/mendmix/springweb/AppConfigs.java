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
package com.mendmix.springweb;

import java.util.List;

import com.mendmix.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : AppConfigs
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class AppConfigs {
	
	public static final String basePackage = ResourceUtils.getProperty("mendmix.application.base-package");

	public static final boolean invokeTokenCheckEnabled = ResourceUtils.getBoolean("mendmix.acl.invoke-token.enabled", true);
	public static final List<String> invokeTokenIgnoreUris = ResourceUtils.getList("mendmix.acl.invoke-token.ignore-uris");

	public static final boolean respRewriteEnabled = ResourceUtils.getBoolean("mendmix.response.rewrite.enbaled", true);
	
	public static final int readTimeout = ResourceUtils.getInt("mendmix.httpclient.readTimeout.ms", 30000);
	
}
