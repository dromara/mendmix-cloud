/**
 * 
 */
package com.jeesuite.cache.redis;

import org.springframework.beans.factory.DisposableBean;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月23日
 */
public interface JedisProvider<S,B> extends DisposableBean{

	public S get();
	
	public B getBinary();
	
	public void release();
	
	public String mode();
	
	public String groupName();
	
	public boolean tenantMode();

}
