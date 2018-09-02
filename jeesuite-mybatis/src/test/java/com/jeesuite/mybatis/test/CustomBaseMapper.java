package com.jeesuite.mybatis.test;

import java.util.List;

import com.jeesuite.mybatis.plugin.cache.annotation.Cache;

import tk.mybatis.mapper.common.MySqlMapper;

public interface CustomBaseMapper<T> extends tk.mybatis.mapper.common.BaseMapper<T>,MySqlMapper<T> {

	@Cache(expire = 300)
	public List<T> findByIds(List<Integer> ids);
}
