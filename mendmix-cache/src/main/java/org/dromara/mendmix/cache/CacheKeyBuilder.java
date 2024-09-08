/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.cache;

import java.util.Collection;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ParameterUtils;


/**
 * 缓存key生成工具
 * <br>
 * Class Name   : CacheKeyBuilder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月9日
 */
public class CacheKeyBuilder {

	
	public static String buildCacheKey(String prefix,Object...args){
		StringBuilder sb = new StringBuilder();
		if(args != null && args.length > 0){
			String argString;
			for (Object arg : args) {
				if(arg == null)continue;
				if(arg.getClass().isEnum() || BeanUtils.isSimpleDataType(arg.getClass())){
					argString = arg.toString();
				}else if(arg.getClass().isArray() || arg instanceof Collection){
					argString = DigestUtils.md5(JsonUtils.toJson(arg));
				}else{
					argString = ParameterUtils.objectToQueryParams(arg);
					if(argString == null)argString = DigestUtils.md5(JsonUtils.toJson(arg));
					if(argString.length() > 32)argString = DigestUtils.md5(argString);
				}
				sb.append(argString).append(GlobalConstants.UNDER_LINE);
			}
		}
		if(sb.length() > 64){
			String md5 = DigestUtils.md5(sb.toString());
			sb.setLength(0);	
			sb.append(md5);
		}
		if(prefix != null){
			sb.insert(0, prefix + GlobalConstants.COLON);
		}
		return sb.toString();
	}
	
	public static String buildCacheKey(Object obj){
		if(BeanUtils.isSimpleDataType(obj.getClass())){
			return obj.toString();
		}else{
			String objString = ParameterUtils.objectToQueryParams(obj);
			if(objString.length() > 32)objString = DigestUtils.md5(objString);
			return objString;
		}
	}
	
	
	public static void main(String[] args) {
		//List<String> primaryGoodsIds = Arrays.asList("1","2");
		String cacheKey = buildCacheKey("xx", "sss",null);
		System.out.println(cacheKey);
	}
}
