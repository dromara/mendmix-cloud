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
package com.mendmix.mybatis.test.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mendmix.mybatis.core.BaseMapper;
import com.mendmix.mybatis.plugin.cache.annotation.Cache;
import com.mendmix.mybatis.test.entity.SnsAccounyBindingEntity;

public interface SnsAccounyBindingEntityMapper extends BaseMapper<SnsAccounyBindingEntity,Integer>{
	
	@Cache(uniqueIndex = true)
	SnsAccounyBindingEntity findBySnsOpenId(@Param("snsType") String snsType,@Param("openId") String openId);
	
	@Cache
	SnsAccounyBindingEntity findByWxUnionIdAndOpenId(@Param("unionId") String unionId,@Param("openId") String openId);
	
	@Cache
	List<SnsAccounyBindingEntity> findByUnionId(@Param("unionId") String unionId);
	
	@Cache(evictOnMethods = {"UserEntityMapper.*"})
	List<SnsAccounyBindingEntity> findByUserId(@Param("userId") int userId);
	
	@Cache
	String findWxUnionIdByUserId(@Param("userId") int userId);
}