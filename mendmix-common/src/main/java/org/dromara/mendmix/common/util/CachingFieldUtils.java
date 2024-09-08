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
package org.dromara.mendmix.common.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jan 2, 2023
 */
public class CachingFieldUtils {

	private static Map<String, Map<String, Field>> fieldCacheHub = new HashMap<>();

	public static Field getField(final Class<?> cls, final String fieldName) {
		String className = cls.getName();
		try {
			Field fileld;
			if (!fieldCacheHub.containsKey(className) || !fieldCacheHub.get(className).containsKey(fieldName)) {
				synchronized (fieldCacheHub) {
					fileld = FieldUtils.getField(cls, fieldName, true);
					if (!fieldCacheHub.containsKey(className)) {
						fieldCacheHub.put(className, new HashMap<>());
					}
					fieldCacheHub.get(className).put(fieldName, fileld);
				}
			} else {
				fileld = fieldCacheHub.get(className).get(fieldName);
			}
			return fileld;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object readField(final Object target, final String fieldName) {
		if (target == null)
			return null;
		Field field = getField(target.getClass(), fieldName);
		if (field == null)
			return null;
		try {
			return field.get(target);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeField(Object target, String fieldName, Object value) {
		if (target == null)
			return;
		Field field = getField(target.getClass(), fieldName);
		if (field == null)
			return;
		try {
			field.set(target, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
