/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.cache.redis.sentinel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.cache.redis.JedisProvider;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * redis哨兵主从模式服务提供者
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisSentinelProvider implements JedisProvider<Jedis,BinaryJedis>{
	

	private static final String SLAVE_CHEKER_KEY = "_slave_cheker";
	private static final String SLAVE_CHEKER_VAL = "1";

	protected static final Logger logger = LoggerFactory.getLogger(JedisSentinelProvider.class);

	
	public static final String MODE = "sentinel";

	private ThreadLocal<Jedis> context = new ThreadLocal<>();
	
	private JedisSentinelPool jedisPool;
	
	private String groupName;
	
	private ScheduledExecutorService failoverCheker;
	
	private boolean tenantModeEnabled; 

	public JedisSentinelProvider(final String groupName,final JedisPoolConfig jedisPoolConfig, String[] servers, final int timeout, final String password, final int database, final String clientName, final String masterName) {
		super();
		this.groupName = groupName;
		final Set<String> sentinels = new HashSet<String>(Arrays.asList(servers));
		
		jedisPool = new JedisSentinelPool(masterName, sentinels, jedisPoolConfig, timeout, password, database,clientName);
	
		failoverCheker = Executors.newScheduledThreadPool(1);
		failoverCheker.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				Jedis jedis = null;
				try {					
					jedis = jedisPool.getResource();
					jedis.set(SLAVE_CHEKER_KEY,SLAVE_CHEKER_VAL);
				} catch (Exception e) {
					if(e instanceof JedisDataException && e.getMessage().contains("READONLY")){
						logger.warn("MENDMIX-TRACE-LOGGGING-->> JedisDataException happend error:{} and will re-init jedisPool" ,e.getMessage());
						//重新初始化jedisPool
						synchronized (jedisPool) {							
							jedisPool.destroy();
							jedisPool = new JedisSentinelPool(masterName, sentinels, jedisPoolConfig, timeout, password, database,clientName);
							logger.info("MENDMIX-TRACE-LOGGGING-->> jedisPool re-init ok,currentHostMaster is:{}:{}" ,jedisPool.getCurrentHostMaster().getHost(),jedisPool.getCurrentHostMaster().getPort());
						}
					}
				}finally {
					try {jedis.close();} catch (Exception e2) {}
				}
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	public Jedis get() throws JedisException {
        Jedis jedis = context.get();
        if(jedis != null)return jedis;
        try {
            jedis = jedisPool.getResource();
        } catch (JedisException e) {
            if(jedis!=null){
            	jedis.close();
            }
            throw e;
        }
        context.set(jedis);
        if(logger.isTraceEnabled()){
        	logger.trace(">>get a redis conn[{}],Host:{}",jedis.toString(),jedis.getClient().getHost());
        }
        return jedis;
    }
 
	@Override
	public BinaryJedis getBinary() {	
		return get();
	}
	
	public void release() {
		Jedis jedis = context.get();
        if (jedis != null) {
        	context.remove();
        	jedis.close();
        	if(logger.isTraceEnabled()){
            	logger.trace("<<release a redis conn[{}]",jedis.toString());
            }
        }
    }

	
	@Override
	public void destroy() throws Exception{
		failoverCheker.shutdown();
		jedisPool.destroy();
	}


	@Override
	public String mode() {
		return MODE;
	}

	@Override
	public String groupName() {
		return groupName;
	}

	public void setTenantModeEnabled(boolean tenantModeEnabled) {
		this.tenantModeEnabled = tenantModeEnabled;
	}
	
	@Override
	public boolean tenantMode() {
		return tenantModeEnabled;
	}

}
