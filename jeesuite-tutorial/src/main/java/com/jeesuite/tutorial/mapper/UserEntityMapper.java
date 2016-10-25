package com.jeesuite.tutorial.mapper;

import com.jeesuite.mybatis.core.BaseMapper;
import com.jeesuite.tutorial.entity.UserEntity;

public interface UserEntityMapper extends BaseMapper<UserEntity, Integer> {
	
	public void update2(UserEntity user);
}