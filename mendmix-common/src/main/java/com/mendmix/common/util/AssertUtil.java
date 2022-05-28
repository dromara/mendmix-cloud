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
package com.mendmix.common.util;

import com.mendmix.common.JeesuiteBaseException;

public class AssertUtil {

	public static void isTrue(boolean expression, int code,String message) {
		if (!expression) {
			throw new JeesuiteBaseException(code, message);
		}
	}
	

	public static void isNull(Object object, String message) {
		if (object != null) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:record.existed", 500), message);
		}
	}

	public static void isNull(Object object) {
		isNull(object, "记录已存在");
	}

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:record.not-exist", 500), message);
		}
	}

	public static void notNull(Object object) {
		notNull(object, "记录不存在");
	}
	

	public static void notBlank(String expression, String message) {
		if (expression == null ||"".equals(expression.trim())) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:param.required", 400), message);
		}
	}

	public static void notBlank(String expression){
		 notBlank(expression, "参数不能为空");
	}

}
