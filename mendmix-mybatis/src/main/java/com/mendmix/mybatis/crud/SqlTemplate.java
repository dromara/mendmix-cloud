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
package com.mendmix.mybatis.crud;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class SqlTemplate {

	public static final String IF_TAG_TEAMPLATE = "<if test=\"%s != null\">%s,</if>";
	public static final String SCRIPT_TEMAPLATE = "<script>%s</script>";
	public static final String TRIM_PREFIX = "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">\n";
	public static final String TRIM_SUFFIX = "</trim>";
	
	public static final String INSERT = "INSERT INTO %s \n %s \n VALUES \n %s";
	public static final String UPDATE_BY_KEY = "UPDATE %s %s \n WHERE %s = #{%s}";
	public static final String BATCH_INSERT = "INSERT INTO %s \n %s \n VALUES \n <foreach collection=\"list\" item=\"item\" index=\"index\" separator=\",\">%s</foreach>";
	public static final String SELECT_BY_KEYS = "SELECT * FROM %s WHERE %s IN  <foreach collection=\"list\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">#{id}</foreach>";
	
	public static String wrapIfTag(String fieldName,String expr,boolean skip){
		if(skip)return expr;
		return String.format(IF_TAG_TEAMPLATE, fieldName,expr);
	}
}
