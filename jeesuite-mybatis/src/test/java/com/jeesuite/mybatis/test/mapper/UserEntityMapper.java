package com.jeesuite.mybatis.test.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.jeesuite.common.model.Page;
import com.jeesuite.common.model.PageParams;
import com.jeesuite.mybatis.core.BaseMapper;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.test.entity.UserEntity;

//@CacheIgnore
public interface UserEntityMapper extends BaseMapper<UserEntity,Integer> {

	@Cache(evictOnMethods = {"updateType","updateType2"})
	List<UserEntity> findByType(short type);

	@Cache
	List<UserEntity> findByStatus(short status);

	@Cache(uniqueIndex = true)
	UserEntity findByMobile(@Param("mobile") String mobile);
	
	@Cache
	UserEntity findByLoginName(@Param("name") String name);

	@Cache
	List<UserEntity> queryByExample(UserEntity user);

	@Cache
	@Select("SELECT count(*) FROM users where type=#{type}")
	int countByType(@Param("type") int type);

	int updateType(@Param("type") int type,@Param("ids") int[] ids);

	void updateByExample(UserEntity user);
	
	int batchDisabled(List<Integer> ids);
	
	@Update("update users set status = 1,updated_at = now() where name = #{name}")
	int updateByName(String name);

	@Cache(expire = 300)
	public List<String> findMobileByIds(List<Integer> ids);

	@Cache
	Page<UserEntity> pageQuery(@Param("pageParam") PageParams pageParam,@Param("example") Map<String, Object> example);

	@Delete("delete from users where mobile = #{mobile}")
	public void delBymobile(@Param("mobile") String mobile);

	@Cache
	UserEntity findByWxUnionId(@Param("unionId") String unionId);

	@Cache(expire=300)
	String findWxUnionIdByUserId(@Param("userId") int userId);
	
	UserEntity findByWxOpenId(String openId);
	
	int updateByMap(Map<String, Object> param);
	
	int updateTypeByExample(@Param("type") int type,@Param("example") UserEntity example);
	
	List<UserEntity> testQuery1(Map<String, Object> param);

}