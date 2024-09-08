package org.dromara.mendmix.example.dao.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.dromara.mendmix.example.dao.entity.ExampleStaffEntity;
import org.dromara.mendmix.mybatis.core.BaseMapper;
import org.dromara.mendmix.mybatis.plugin.cache.annotation.Cache;

public interface ExampleStaffEntityMapper extends BaseMapper<ExampleStaffEntity, Integer> {
	
	@Cache
	@Select("SELECT count(*) FROM staff where type=#{type}")
	int countByType(@Param("type") int type);
	
	@Cache
	@Select("SELECT * FROM staff WHERE mobile=#{mobile} AND enabled=1 LIMIT 1")
	@ResultMap("BaseResultMap")
	ExampleStaffEntity findByMobile(String mobile);
	
	List<ExampleStaffEntity> findListByParam(Map<String, Object> param);
}