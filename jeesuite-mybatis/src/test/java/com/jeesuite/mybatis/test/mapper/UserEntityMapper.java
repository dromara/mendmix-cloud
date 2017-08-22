package com.jeesuite.mybatis.test.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.pagination.Page;
import com.jeesuite.mybatis.plugin.pagination.PageParams;
import com.jeesuite.mybatis.plugin.pagination.annotation.Pageable;
import com.jeesuite.mybatis.test.entity.UserEntity;

import tk.mybatis.mapper.common.BaseMapper;
import tk.mybatis.mapper.common.ExampleMapper;
import tk.mybatis.mapper.common.MySqlMapper;

public interface UserEntityMapper extends BaseMapper<UserEntity>,ExampleMapper<UserEntity>,MySqlMapper<UserEntity> {
	
	@Cache
	List<UserEntity> findByType(short type);
	
	@Cache
	@Pageable
	List<UserEntity> findByStatus(short status);
	
	@Cache
	UserEntity findByMobile(@Param("mobile") String mobile);
	
	@Cache
	@Pageable
	List<UserEntity> queryByExample(UserEntity user);
	
	@Cache
	int countByExample(UserEntity user);
	
	@Cache
	@Select("SELECT count(*) FROM users where type=#{type}")
    int countByType(@Param("type") int type);
	
	int updateType(@Param("ids") int[] ids,@Param("type") int type);
	
	void updateType2(UserEntity user);
	
	@Cache(expire=300)
	public List<String> findMobileByIds(List<Integer> ids);
	
	@Select("SELECT * FROM users where 1=1")
	@ResultMap("BaseResultMap")
    Page<UserEntity> pageQuery(@Param("pageParam") PageParams pageParam);
	
	@Delete("delete from users where mobile = #{mobile}")
	public void delBymobile(@Param("mobile") String mobile);
	
}