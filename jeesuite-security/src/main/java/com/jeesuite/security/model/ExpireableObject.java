package com.jeesuite.security.model;

/**
 * 
 * 
 * <br>
 * Class Name   : ExpireableObject
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 1, 2021
 */
public class ExpireableObject {

	private Object target;
	private long expireAt;
	
	
	public ExpireableObject() {}
	
	public ExpireableObject(Object target, long expireAt) {
		super();
		this.target = target;
		this.expireAt = expireAt;
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
