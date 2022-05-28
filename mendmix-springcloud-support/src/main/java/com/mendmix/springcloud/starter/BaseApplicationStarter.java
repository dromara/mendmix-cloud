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
package com.mendmix.springcloud.starter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.async.AsyncInitializer;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.logging.integrate.LogProfileManager;
import com.mendmix.spring.ApplicationStartedListener;
import com.mendmix.spring.InstanceFactory;

public class BaseApplicationStarter {

	protected static long before(String... args) {
		if (args != null) {
			ApplicationArguments arguments = new DefaultApplicationArguments(args);
			Set<String> optionNames = arguments.getOptionNames();

			for (String name : optionNames) {
				List<String> values = arguments.getOptionValues(name);
				if (values != null && !values.isEmpty()) {
					System.setProperty(name, values.get(0));
					System.out.println(String.format("add ApplicationArguments: %s = %s", name, values.get(0)));
				}
			}
		}
		LogProfileManager.initialize();
		System.setProperty("client.nodeId", GlobalRuntimeContext.getNodeName());
		return System.currentTimeMillis();
	}

	protected static void after(long starTime) {
		//
		LogProfileManager.reload();

		long endTime = System.currentTimeMillis();
		long time = endTime - starTime;
		System.out.println("\nStart Time: " + time / 1000 + " s");
		System.out.println("...............................................................");
		System.out.println("..................Service starts successfully (port:" + ResourceUtils.getProperty("server.port") + ")..................");
		System.out.println("...............................................................");

		// 执行异步初始化
		Map<String, AsyncInitializer> asyncInitializers = InstanceFactory.getBeansOfType(AsyncInitializer.class);
		if (asyncInitializers != null) {
			for (AsyncInitializer initializer : asyncInitializers.values()) {
				initializer.process();
			}
		}
		Map<String, ApplicationStartedListener> interfaces = InstanceFactory
				.getBeansOfType(ApplicationStartedListener.class);
		if (interfaces != null) {
			for (ApplicationStartedListener listener : interfaces.values()) {
				listener.onApplicationStarted(InstanceFactory.getContext());
			}
		}

	}
}
