package com.jeesuite.mybatis.plugin.cache.provider;

import java.io.IOException;
import java.util.List;

import com.jeesuite.mybatis.plugin.cache.CacheProvider;

public class NullCacheProvider implements CacheProvider {

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> T get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStr(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean set(String key, Object value, long expireSeconds) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setStr(String key, Object value, long expireSeconds) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(String... keys) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean exists(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setExpire(String key, long expireSeconds) {
		// TODO Auto-generated method stub

	}

	@Override
	public void putGroup(String cacheGroupKey, String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearGroup(String groupName, String... prefixs) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getListItems(String key, int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getListSize(String key) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean setnx(String key, String value, long expireSeconds) {
		// TODO Auto-generated method stub
		return false;
	}

}
