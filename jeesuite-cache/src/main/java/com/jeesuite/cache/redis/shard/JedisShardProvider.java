/**
 * 
 */
package com.jeesuite.cache.redis.shard;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.redis.JedisProvider;

import redis.clients.jedis.BinaryShardedJedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 标准（单服务器）redis服务提供者
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月23日
 */
public class JedisShardProvider implements JedisProvider<ShardedJedis,BinaryShardedJedis>{
	
	protected static final Logger logger = LoggerFactory.getLogger(JedisShardProvider.class);

	
	public static final String MODE = "standard";

	private ThreadLocal<ShardedJedis> context = new ThreadLocal<>();
	
	private ShardedJedisPool jedisPool;
	
	private String groupName;
	

	public JedisShardProvider(String groupName,JedisPoolConfig jedisPoolConfig, String[] servers, int timeout) {
		super();
		this.groupName = groupName;
		List<JedisShardInfo> shards = buildShardInfos(servers,timeout); 
		jedisPool = new ShardedJedisPool(jedisPoolConfig, shards);
	}
	
	private List<JedisShardInfo> buildShardInfos(String[] servers, int timeout){
		List<JedisShardInfo> infos = new ArrayList<>();
		for (String server : servers) {
			String[] addrs = server.split(":");
			JedisShardInfo info = new JedisShardInfo(addrs[0], Integer.parseInt(addrs[1].trim()), timeout);
			infos.add(info);
		}
		
		return infos;
	}

	public ShardedJedis get() throws JedisException {
		ShardedJedis jedis = context.get();
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
        	logger.trace(">>get a redis conn[{}]",jedis.toString());
        }
        return jedis;
    }
 
	@Override
	public BinaryShardedJedis getBinary() {	
		return get();
	}
	
	public void release() {
		ShardedJedis jedis = context.get();
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
