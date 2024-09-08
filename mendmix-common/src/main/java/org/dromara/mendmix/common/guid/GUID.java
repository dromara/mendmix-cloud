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
package org.dromara.mendmix.common.guid;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class GUID {

	private static final String LINE_THROUGH = "-";
	
	private static TimestampGUIDGenarator genarator = new TimestampGUIDGenarator(9999);
	private static SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
	
	public static String uuid(){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		return str;
	}
	
	public static String guidWithTimestamp(String...prefixs){
		return genarator.next(prefixs);
	}
	
	public static long guid(){
		return snowflakeGenerator.nextId();
	}
	
	
}
