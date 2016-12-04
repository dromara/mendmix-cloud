package com.jeesuite.mybatis.test.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jeesuite.mybatis.core.BaseMapper;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.test.entity.UserEntity;

public interface UserEntityMapper extends BaseMapper<UserEntity, Integer> {
	
	@Cache
	List<UserEntity> findByType(short type);
	
	@Cache
	List<UserEntity> findByStatus(short status);
	
	@Cache
	UserEntity findByMobile(@Param("mobile") String mobile);
	
}