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
package com.mendmix.common2.workerid;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.WorkIdGenerator;
import com.mendmix.common.util.DigestUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 21, 2022
 */
public class LocalWorkIdGenerator implements WorkIdGenerator {

	
	public LocalWorkIdGenerator() {
		GlobalRuntimeContext.setWorkIdGenerator(this);
	}

	@Override
	public int generate(String nodeId) {
		File dataDir = GlobalRuntimeContext.getAppDataDir();
		if(!dataDir.exists()) {
			return RandomUtils.nextInt(10, 99);
		}
		try {
			File file = new File(dataDir,DigestUtils.md5(nodeId) + ".wid");
			if(file.exists()) {
				String content = FileUtils.readFileToString(file,StandardCharsets.UTF_8);
				return Integer.parseInt(content.trim());
			}
			int workId = RandomUtils.nextInt(10, 99);
			FileUtils.write(file, String.valueOf(workId), StandardCharsets.UTF_8, false);
			return workId;
		} catch (Exception e) {
			return RandomUtils.nextInt(10, 99);
		}
	}

}
