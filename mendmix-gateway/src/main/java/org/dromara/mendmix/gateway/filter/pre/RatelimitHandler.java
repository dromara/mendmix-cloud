/**
 * 
 */
package org.dromara.mendmix.gateway.filter.pre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.exception.MainErrorType;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.task.SubTimerTask;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.filter.FakeResponseHandler;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.model.BizSystem;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.gateway.model.RatelimitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;

/**
 * <br>
 * 
 * @author vakinge
 * @date 2023年9月4日
 */
public class RatelimitHandler implements FakeResponseHandler, CommandLineRunner, SubTimerTask {

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");
	private static final String ANY_URI_WILDCARD = "/*";
	
	private String fetchUrl = "http://paas-systemmgt-svc/ratelimitRule/findBySystemIds";
	
	private Cache<String,RateLimiter> rateLimiterHub = CacheBuilder
			.newBuilder()
			.maximumSize(ResourceUtils.getLong("mendmix-cloud.governance.ratelimit.maxCacheSize", 30000))
			.expireAfterWrite(ResourceUtils.getInt("mendmix-cloud.governance.ratelimit.maxCacheMinutes", 15), TimeUnit.MINUTES)
			.build();
	
	//<serviceId,<url,rule>>
	private Map<String, Map<String, RatelimitStrategy>> serviceStrategies = new ConcurrentHashMap<>();
	private Map<String, Map<Pattern, RatelimitStrategy>> wildcardServiceStrategies = new ConcurrentHashMap<>();
	
	@Autowired(required=false)
	private DiscoveryClient discoveryClient;
	
	private int activePodNums = 1;

	@Override
	public Object handle(ServerWebExchange exchange, boolean preHandle) {
		RateLimiter limiter = getRateLimiter(exchange);
		boolean getPermit = limiter == null || limiter.tryAcquire();
		if(!getPermit) {
			throw new MendmixBaseException(MainErrorType.TOO_MANY_REQUEST_LIMIT);
		}
		if(RequestContextHelper.isDebugMode(exchange) && limiter != null) {
			logger.info("<TRACE-LOGGGING> uri:{} 获得限流锁...,limiterTotal:{}",RequestContextHelper.getOriginRequestUri(exchange),rateLimiterHub.size());
		}
		return null;
	}
	
	private RateLimiter getRateLimiter(ServerWebExchange exchange) {
		ApiInfo apiInfo = RequestContextHelper.getCurrentApi(exchange);
		if(apiInfo == null)return null;
		BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		//优先动态规则
		RatelimitStrategy strategy = findActiveRuleByUri(module.getServiceId(), apiInfo.getIdentifier());
		if(strategy == null) {
			return null;
		}
		String resolveHitKey = strategy.resolveHitKey(exchange,module.getServiceId(),apiInfo);
		if(resolveHitKey != null) {
			RateLimiter rateLimiter = rateLimiterHub.getIfPresent(resolveHitKey);
			if(rateLimiter != null)return rateLimiter;
			synchronized (rateLimiterHub) {
				rateLimiter = rateLimiterHub.getIfPresent(resolveHitKey);
				if(rateLimiter != null)return rateLimiter;
				double permitsPerSecond = strategy.getPermitsPerSecond();
				if(activePodNums > 1) {
					permitsPerSecond = permitsPerSecond / activePodNums;
				}
				rateLimiter = RateLimiter.create(permitsPerSecond);
				rateLimiterHub.put(resolveHitKey, rateLimiter);
				return rateLimiter;
			}
		}
		return null;
	}

	public List<RatelimitStrategy> findRatelimitStrategies(String serviceId){
		List<RatelimitStrategy> rules = new ArrayList<>();
		if(serviceStrategies.containsKey(serviceId)) {
			rules.addAll(serviceStrategies.get(serviceId).values());
		}
		if(wildcardServiceStrategies.containsKey(serviceId)) {
			rules.addAll(wildcardServiceStrategies.get(serviceId).values());
		}
		return rules;
	}
	
    public String saveRatelimitStrategy(RatelimitStrategy strategy) {
    	String serviceId = strategy.getServiceId();
    	if(StringUtils.isAnyBlank(serviceId,strategy.getHitType(),strategy.getUriKey())) {
    		return null;
    	}
    	
    	BizSystemModule module = CurrentSystemHolder.getModuleByServiceId(strategy.getServiceId());
    	if(module == null) {
    		throw new MendmixBaseException("服务模块["+serviceId+"]不存在");
    	}
    	
        if(StringUtils.isBlank(strategy.getId())) {
        	strategy.setId(DigestUtils.md5(serviceId + strategy.getHitType() + strategy.getUriKey()));
    	}
        //根据id找到原来的规则
        removeRatelimitStrategy(serviceId, strategy.getId());
    	
    	Map<String, RatelimitStrategy> uriRuleMapping = serviceStrategies.get(serviceId);
    	if(uriRuleMapping == null) {
    		serviceStrategies.put(serviceId, uriRuleMapping = new HashMap<>());
    	}
    	
    	Pattern pattern = strategy.getUriPattern();
		if(pattern == null) {
			uriRuleMapping.put(strategy.getUriKey(), strategy);
		}else {
			Map<Pattern, RatelimitStrategy> patternRuleMapping = wildcardServiceStrategies.get(serviceId);
		    if(patternRuleMapping == null) {
		    	wildcardServiceStrategies.put(serviceId, patternRuleMapping = new HashMap<>());
		    }
		    patternRuleMapping.put(pattern, strategy);
		}
        
    	removeRateLimiters(serviceId);
    	
    	return strategy.getId();
	}
    
    public void removeRatelimitStrategy(String serviceId,String id) {
    	RatelimitStrategy strategy = findActiveRuleById(serviceId, id);
        if(strategy != null) {
        	if(strategy.getUriPattern() == null) {
        		serviceStrategies.get(strategy.getServiceId()).remove(strategy.getUriKey());
        	}else {
        		wildcardServiceStrategies.get(strategy.getServiceId()).remove(strategy.getUriPattern());
        	}
        }
        removeRateLimiters(serviceId);
    }
    
    public void removeRatelimitStrategy(RatelimitStrategy strategy) {
    	if(StringUtils.isBlank(strategy.getId())) {
        	strategy.setId(DigestUtils.md5(strategy.getServiceId() + strategy.getHitType() + strategy.getUriKey()));
    	}
    	removeRatelimitStrategy(strategy.getServiceId(), strategy.getId());
    	
    }
    
    private RatelimitStrategy findActiveRuleById(String serviceId,String id) {
    	RatelimitStrategy rule = null;
    	if(serviceStrategies.containsKey(serviceId)) {
    		rule = serviceStrategies.get(serviceId).values().stream().filter(o -> {
    			return o.getId().equals(id);
    		}).findFirst().orElse(null);
    	}
    	
    	if(rule == null && wildcardServiceStrategies.containsKey(serviceId)) {
    		rule = wildcardServiceStrategies.get(serviceId).values().stream().filter(o -> {
    			return o.getId().equals(id);
    		}).findFirst().orElse(null);
    	}
    	return rule;
    }
    
    private RatelimitStrategy findActiveRuleByUri(String serviceId,String uri) {
    	RatelimitStrategy rule = null;
    	if(serviceStrategies.containsKey(serviceId)) {
    		rule = serviceStrategies.get(serviceId).values().stream().filter(o -> {
    			return o.getUriKey().equals(uri);
    		}).findFirst().orElse(null);
    	}
    	
    	if(rule == null && wildcardServiceStrategies.containsKey(serviceId)) {
    		rule = wildcardServiceStrategies.get(serviceId).values().stream().filter(o -> {
    			return o.getUriPattern().matcher(uri).matches();
    		}).findFirst().orElse(null);
    	}
    	
    	if(rule == null && serviceStrategies.containsKey(serviceId)) {
    		rule = serviceStrategies.get(serviceId).get(ANY_URI_WILDCARD);
    	}
    	return rule;
    }
   
    private void removeRateLimiters(String...serviceIds) {
    	if(serviceIds == null) {
    		rateLimiterHub.invalidateAll();
    	}else {
    		Set<String> keys = rateLimiterHub.asMap().keySet();
    		for (String key : keys) {
				for (String serviceId : serviceIds) {
					if(key.startsWith(serviceId)) {
						rateLimiterHub.invalidate(key);
					}
				}
			}
    	}
    }

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void run(String... args) throws Exception {
		//静态规则
		List<RatelimitStrategy> staticStrategies = ResourceUtils.getConfigObjects("mendmix-cloud.governance.ratelimit.rule", RatelimitStrategy.class);
	    int index = 1;
		for (RatelimitStrategy strategy : staticStrategies) {
	    	strategy.setId(GatewayConstants.STATIC_RULE_ID_PREFIX + index);
	    	strategy.setUriKey(strategy.getUriKey());
	    	try {				
				saveRatelimitStrategy(strategy);
			} catch (Exception e) {
				logger.warn(">>initRemoteRule:{},error:{}",strategy.getUriKey(),e.getMessage());
			}
	    	index++;
		}
	}

	@Override
	public void doSchedule() {
		if(discoveryClient != null) {
			int newPodNums = discoveryClient.getInstances(GlobalContext.APPID).size();
			if(activePodNums != newPodNums) {
				activePodNums = newPodNums;
				rateLimiterHub.invalidateAll();
			}
		}
		initRemoteRules();
	}
	
	public synchronized void initRemoteRules() {
		if(!serviceStrategies.isEmpty() || !wildcardServiceStrategies.isEmpty())return;
		List<String> systemIds = CurrentSystemHolder.getSystems().stream().map(BizSystem::getId).collect(Collectors.toList());
		List<RatelimitStrategy> list = HttpRequestEntity.post(fetchUrl).backendInternalCall().fallbackHitCache().objectBody(systemIds).execute().toList(RatelimitStrategy.class);
		for (RatelimitStrategy rule : list) {
			try {				
				saveRatelimitStrategy(rule);
			} catch (Exception e) {
				logger.warn(">>initRemoteRule:{},error:{}",rule.getUriKey(),e.getMessage());
			}
		}
		if(!list.isEmpty()) {
			logger.debug(">>init GrayRoute Rules:{}",JsonUtils.toJson(list));
		}
	}


	@Override
	public long interval() {
		return 60 * 1000;
	}

}
