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
package org.dromara.mendmix.springweb;

import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 3, 2023
 */
public class GlobalContexInitializer implements EnvironmentPostProcessor {

	public static void preArguments(String... args) {
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
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		
	}

}
