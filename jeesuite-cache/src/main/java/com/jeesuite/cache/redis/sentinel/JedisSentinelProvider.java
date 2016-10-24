/**
 * 
 */
package com.jeesuite.cache.redis.sentinel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.redis.JedisProvider;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * redis哨兵主从模式服务提供者
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月26日
 */
public class JedisSentinelProvider implements JedisProvider<Jedis,BinaryJedis>{
	
	protected static final Logger logger = LoggerFactory.getLogger(JedisSentinelProvider.class);

	
	public static final String MODE = "sentinel";

	private ThreadLocal<Jedis> context = new ThreadLocal<>();
	
	private JedisSentinelPool jedisPool;
	
	private String groupName;
	

	public JedisSentinelProvider(String groupName,JedisPoolConfig jedisPoolConfig, String[] servers, int timeout, String password, int database, String clientName, String masterName) {
		super();
		this.groupName = groupName;
		Set<String> sentinels = new HashSet<String>(Arrays.asList(servers));
		
		jedisPool = new JedisSentinelPool(masterName, sentinels, jedisPoolConfig, timeout, password, database,clientName);
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

}
