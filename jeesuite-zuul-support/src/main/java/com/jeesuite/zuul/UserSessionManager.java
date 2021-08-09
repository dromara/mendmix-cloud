package com.jeesuite.zuul;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.cache.redis.JedisProvider;
import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.cache.redis.sentinel.JedisSentinelProvider;
import com.jeesuite.cache.redis.standalone.JedisStandaloneProvider;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.AssertUtil;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.zuul.cache.MemoryCache;
import com.jeesuite.zuul.cache.RedisCache;
import com.jeesuite.zuul.model.UserSession;
import com.jeesuite.springweb.CurrentRuntimeContext;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 
 * <br>
 * Class Name   : UserSessionManager
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月10日
 */
public class UserSessionManager{
	

	private static Logger log = LoggerFactory.getLogger("com.jeesuite.zuul.auth");

	private static final String SESSION_CACHE_NAME = "sessionCache";
	private static final String PERM_CACHE_NAME = "permCache";
	private static final String TEMP_CACHE_NAME = "temporaryCache";
	private static final String CACHE_GROUP = "_redisAuth";
	private static int sessionExpireIn = 0;

	private Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	
	private String cacheType = ResourceUtils.getProperty("auth.storage.type", "memory");

	private static UserSessionManager instance;

	private UserSessionManager() {
		init();
		//
		ApiPermissionHolder.loadApiPermissions();
	}

	/**
	 * @return the instance
	 */
	private static UserSessionManager getInstance() {
		if(instance != null)return instance;
		synchronized (UserSessionManager.class) {
			if(instance != null)return instance;
			instance = new UserSessionManager();
		}
		return instance;
	}

	private void addCahe(String cacheName,int timeToLiveSeconds){
		Cache cache = null;
		if("redis".equals(cacheType)){
			cache = new RedisCache(CACHE_GROUP,cacheName,timeToLiveSeconds);
		}else{
			cache = new MemoryCache(timeToLiveSeconds);
		}
		caches.put(cacheName, cache);
	}
	
	private void init() {
		log.info(">>storage.type:{}",cacheType);
		if("redis".equals(cacheType)){
			String mode = ResourceUtils.getProperty("auth.redis.mode", ResourceUtils.getProperty("jeesuite.cache.mode"));
			String server = ResourceUtils.getProperty("auth.redis.servers", ResourceUtils.getProperty("jeesuite.cache.servers"));
			String datebase = ResourceUtils.getProperty("auth.redis.datebase", ResourceUtils.getProperty("jeesuite.cache.datebase","0"));
			String password = ResourceUtils.getProperty("auth.redis.password", ResourceUtils.getProperty("jeesuite.cache.password"));
			String maxPoolSize = ResourceUtils.getProperty("auth.redis.maxPoolSize", ResourceUtils.getProperty("jeesuite.cache.maxPoolSize","100"));
			
			Validate.notBlank(server, "config[auth.redis.servers] not found");
			
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxIdle(1);
			poolConfig.setMinEvictableIdleTimeMillis(30*60*1000);
			poolConfig.setMaxTotal(Integer.parseInt(maxPoolSize));
			poolConfig.setMaxWaitMillis(5 * 1000);
			
			String[] servers = server.split(";|,");
			JedisProvider<Jedis,BinaryJedis> provider;
			if("sentinel".equals(mode)){
				String masterName = ResourceUtils.getProperty("auth.redis.masterName", ResourceUtils.getProperty("jeesuite.cache.masterName"));
				AssertUtil.notBlank(masterName,"config[auth.redis.masterName] not found");
				provider = new JedisSentinelProvider(CACHE_GROUP, poolConfig, servers, 30000, password, Integer.parseInt(datebase), null, masterName);
			}else{
				provider = new JedisStandaloneProvider(CACHE_GROUP, poolConfig, servers, 30000, password, Integer.parseInt(datebase),null);
			}
			JedisProviderFactory.addProvider(provider);
			log.info("redis cache group created -> server:{},mode:{},datebase:{}",server,mode,datebase);
		}
		
		int expireIn = SessionCookieUtil.SESSION_EXPIRE_IN > 0 ? SessionCookieUtil.SESSION_EXPIRE_IN : 7200;
		addCahe(SESSION_CACHE_NAME, expireIn);
		addCahe(PERM_CACHE_NAME,expireIn);
		//temporary
		addCahe(TEMP_CACHE_NAME,180);
		sessionExpireIn = expireIn - 5;
	}
	
	public static Cache getSessionCache(){
		return getInstance().caches.get(SESSION_CACHE_NAME);
	}

	public static Cache getPermCache(){
		return getInstance().caches.get(PERM_CACHE_NAME);
	}
	
	public static int getSessionExpireIn() {
		return sessionExpireIn;
	}
	
	public static UserSession storageSession(AuthUser ahthUser){
		
		String sessionId = getSessionId();

		UserSession session = null;
		boolean isRepeatLogin = false;
		//TODO 标识不同租户，不同系统
		String sessionMappingKey = ahthUser.getId();
		String historySessionId = getSessionCache().getString(sessionMappingKey);
		if(StringUtils.isNotBlank(historySessionId)){
			isRepeatLogin = StringUtils.equals(sessionId, historySessionId);
			if(!isRepeatLogin){
				destorySeesion(historySessionId);
			}else{
				log.info("RepeatLogin sessionId:{}",sessionId);
				session = getSessionCache().getObject(sessionId);
			}
		}
		
		if(session == null){
			session = new UserSession(sessionId, ahthUser);
			session.setExpiredAt(System.currentTimeMillis() + sessionExpireIn * 1000);
			getSessionCache().setString(sessionMappingKey, sessionId);
			getSessionCache().setObject(sessionId, session);
		}

		getPermissions(session);
	
		CurrentRuntimeContext.setAuthUser(ahthUser);
		
		return session;
	}
	

	public static UserSession getUserSession(){
		
		String sessionId = getSessionId();
		
		if(StringUtils.isBlank(sessionId))return null;
		UserSession session = getSessionCache().getObject(sessionId);
		if(session == null)return null;
		//权限
		getPermissions(session);
		return session;
	}
	

	public static void destorySeesion(){
		String sessionId = getSessionId();
		if(sessionId == null)return;
		destorySeesion(sessionId);
	}
	
	public static AuthUser getAuthUser(){
		AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
		if(authUser != null)return authUser;

		UserSession loginSession = getUserSession();
		if(loginSession == null)return null;
		
		authUser = loginSession.getUser();
		
		CurrentRuntimeContext.setAuthUser(authUser);
		
		return authUser;
	}
	
	public static void setSessionAttribute(String name,Object object) {
		Cache cache = getInstance().caches.get(TEMP_CACHE_NAME);
		String sessionId = getSessionId();
		cache.setMapValue(sessionId, name, object);
    }
    
    public static <T> T getSessionAttribute(String name) {
    	Cache cache = getInstance().caches.get(TEMP_CACHE_NAME);
		String sessionId = getSessionId();
		return cache.getMapValue(sessionId, name);
    } 
    
    private static String getSessionId() {
		HttpServletRequest request = CurrentRuntimeContext.getRequest();
		HttpServletResponse response = CurrentRuntimeContext.getResponse();
		
		return SessionCookieUtil.getSessionId(request, response);
	}
    
    private static void destorySeesion(String sessionId){
		UserSession session = getSessionCache().getObject(sessionId);
		if(session == null)return;
		getSessionCache().remove(session.getSessionId());
		getSessionCache().remove(sessionId);
		getPermCache().remove(sessionId);
		log.debug("logout -> sessionId:{}",sessionId);
	}
	
	private static List<String> getPermissions(UserSession session){
		List<String> permissions = getPermCache().getObject(session.getSessionId());
		if(permissions == null){
			synchronized (getInstance()) {
				permissions = ServiceInstances.systemMgrApi().findUserApiPermissions(session.getUser().getId())
                           .stream() //
                           .map(e -> {return ApiPermissionHolder.buildPermissionKey(e.getHttpMethod(), e.getUri());}) //
                           .collect(Collectors.toList());
				getPermCache().setObject(session.getSessionId(), permissions);
				log.debug("initPermissions OK -> sessionId:{},permissionNum:{} ",session.getSessionId(),permissions.size());
			}
		}
		//
		session.setPermissions(permissions);
		
		return permissions;
	}

}
