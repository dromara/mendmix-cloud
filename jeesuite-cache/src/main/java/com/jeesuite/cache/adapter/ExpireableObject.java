package com.jeesuite.cache.adapter;

public class ExpireableObject {

	private Object target;
	private long expireAt;
	
	
	public ExpireableObject() {}
	
	public ExpireableObject(Object target, long timeout) {
		this.target = target;
		this.expireAt = System.currentTimeMillis() + timeout * 1000;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public long getExpireAt() {
		return expireAt;
	}

	public void setExpireAt(long expireAt) {
		this.expireAt = expireAt;
	}

}
