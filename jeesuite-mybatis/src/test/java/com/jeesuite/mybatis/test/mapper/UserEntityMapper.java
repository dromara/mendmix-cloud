package com.jeesuite.mybatis.test.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.jeesuite.mybatis.core.BaseMapper;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.pagination.Page;
import com.jeesuite.mybatis.plugin.pagination.PageParams;
import com.jeesuite.mybatis.plugin.pagination.annotation.Pageable;
import com.jeesuite.mybatis.test.entity.UserEntity;

public interface UserEntityMapper extends BaseMapper<UserEntity,Integer> {

	@Cache(evictOnMethods = "*")
	List<UserEntity> findByType(short type);

	@Cache
	@Pageable
	List<UserEntity> findByStatus(short status);

	@Cache
	UserEntity findByMobile(@Param("mobile") String mobile);
	
	@Cache
	UserEntity findByLoginName(@Param("name") String name);

	@Cache
	@Pageable
	List<UserEntity> queryByExample(UserEntity user);

	@Cache
	int countByExample(UserEntity user);

	@Cache
	@Select("SELECT count(*) FROM users where type=#{type}")
	int countByType(@Param("type") int type);

	int updateType(@Param("ids") int[] ids, @Param("type") int type);

	void updateType2(UserEntity user);

	@Cache(expire = 300)
	public List<String> findMobileByIds(List<Integer> ids);

	Page<UserEntity> pageQuery(@Param("pageParam") PageParams pageParam,@Param("example") Map<String, Object> example);

	@Delete("delete from users where mobile = #{mobile}")
	public void delBymobile(@Param("mobile") String mobile);

	@Cache
	UserEntity findByWxUnionId(@Param("unionId") String unionId);

	@Cache(expire=300)
	String findWxUnionIdByUserId(@Param("userId") int userId);

}