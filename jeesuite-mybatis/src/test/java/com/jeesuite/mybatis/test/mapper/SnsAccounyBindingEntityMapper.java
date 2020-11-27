package com.jeesuite.mybatis.test.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jeesuite.mybatis.core.BaseMapper;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.test.entity.SnsAccounyBindingEntity;

public interface SnsAccounyBindingEntityMapper extends BaseMapper<SnsAccounyBindingEntity,Integer>{
	
	@Cache(uniqueIndex = true)
	SnsAccounyBindingEntity findBySnsOpenId(@Param("snsType") String snsType,@Param("openId") String openId);
	
	@Cache
	SnsAccounyBindingEntity findByWxUnionIdAndOpenId(@Param("unionId") String unionId,@Param("openId") String openId);
	
	@Cache
	List<SnsAccounyBindingEntity> findByUnionId(@Param("unionId") String unionId);
	
	@Cache
	List<SnsAccounyBindingEntity> findByUserId(@Param("userId") int userId);
	
	@Cache
	String findWxUnionIdByUserId(@Param("userId") int userId);
}