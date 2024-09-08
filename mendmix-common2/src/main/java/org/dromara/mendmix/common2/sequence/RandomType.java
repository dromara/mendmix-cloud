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
package org.dromara.mendmix.common.sequence;

/**
 * 
 * <br>
 * Class Name   : RandomType
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月12日
 */
public enum RandomType {

	/**
	 * 字符
	 */
	CHAR,

	/**
	 * 数字
	 */
	NUMBER,

	/**
	 * 任意
	 */
	ANY;
	
	public static boolean numbers(String value){
		return RandomType.NUMBER.name().equalsIgnoreCase(value) || RandomType.ANY.name().equalsIgnoreCase(value);
	}
	
	public static boolean chars(String value){
		return RandomType.CHAR.name().equalsIgnoreCase(value) || RandomType.ANY.name().equalsIgnoreCase(value);
	}
}
