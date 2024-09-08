package org.dromara.mendmix.gateway.filter.pre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.cache.adapter.ExpireableObject;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.constants.ValueType;
import org.dromara.mendmix.common.exception.MainErrorType;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.model.Page;
import org.dromara.mendmix.common.model.PageParams;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.GatewayConstants.FallbackStrategy;
import org.dromara.mendmix.gateway.filter.BreakerCondition;
import org.dromara.mendmix.gateway.filter.FakeResponseHandler;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.dromara.mendmix.gateway.helper.RequestFallbackHelper;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.gateway.model.FallbackRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月19日
 */
public class FallbackBreakerHandler implements FakeResponseHandler,InitializingBean {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.edge");
	
	private static final String CTX_FALLBACK_RULE_KEY = "__ctx_FallbackRule";
	private static final String FALLBACK_RULE_KEY_PRIFIX = String.format("fallbackRule:%s:", GlobalContext.SYSTEM_KEY);
	private static final String FALLBACK_RULE_ID_MAPPING_PREFIX = String.format("fallbackRuleIdMapping:%s:", GlobalContext.SYSTEM_KEY);
	private static final String URI_REL_FALLBACK_RULE_MAPPING_KEY_PRIFIX = String.format("fallbackRuleUriMapping:%s:", GlobalContext.SYSTEM_KEY);
	
	private static final String ANY_URI_WILDCARD = "/*";
	
	private List<FallbackRule> blankRules = new ArrayList<>(0);
	//<servieId,<uri,rule[]>>
	private Map<String, Map<String, List<FallbackRule>>> staticRules = new HashMap<>();
	
	private Cache<String,Object> level1Cache =  CacheBuilder
			.newBuilder()
			.maximumSize(10000)
			.weakValues()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	
	@Autowired(required = false)
	private BreakerCondition breakerCondition;

	@Override
	public Object handle(ServerWebExchange exchange,boolean preHandle) {
		
		final BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		if(module.isGateway()) {
			return null;
		}
		boolean traceLogging = logger.isTraceEnabled() || exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME);
		ApiInfo apiInfo = RequestContextHelper.getCurrentApi(exchange);
		if(breakerCondition != null && !breakerCondition.match(exchange, module,apiInfo)) {
			if(traceLogging)logger.info("<trace_loggging> breakerCondition matched uri:{}",RequestContextHelper.getOriginRequestUri(exchange));
			return null;
		}
		try {
			List<FallbackRule> matchedRules = matchFallbackRules(exchange, module.getServiceId(), apiInfo);
			if(matchedRules == null || matchedRules.isEmpty())return null;
	        //
			Optional<FallbackRule> optional = matchedRules.stream().filter(rule -> {
				if(rule.getStrategy() == null)return false;
				if(preHandle && !rule.isBreakerMode())return false;
				return rule.currentMatch(exchange);
			}).findFirst();
			if(!optional.isPresent())return null;
			
			FallbackRule rule = optional.get();
			if(traceLogging) {
				logger.info("<trace_loggging> request[{}] fallback -> usingRule:{}",RequestContextHelper.getOriginRequestUri(exchange),JsonUtils.toJson(rule));
			}
			String requestURI = RequestContextHelper.getOriginRequestUri(exchange);
			if (FallbackStrategy.hitCache.name().equals(rule.getStrategy())) {
				String hitKey = rule.getFallbackSourceKey();
				if(hitKey == null)hitKey = RequestContextHelper.getCurrentRequestHitKey(exchange,true);
				if(traceLogging) {
					logger.info("<trace_loggging> request[{}] fallback -> hitCacheKey:{}",requestURI,hitKey);
				}
				String cacheData = CacheUtils.getFallbackCache(hitKey);
				if (cacheData != null) {
					Map<String, Object> data = JsonUtils.toHashMap(cacheData, Object.class);
					return data;
				}
			}else if(FallbackStrategy.forwardBackup.name().equals(rule.getStrategy())) {
				return RequestFallbackHelper.handleForwardUrl(exchange, rule.getFallbackSourceKey());
			}else if(FallbackStrategy.returnBlank.name().equals(rule.getStrategy())) {
				Map<String, Object> result = new HashMap<>(2);
				result.put(GlobalConstants.PARAM_CODE, 200);
				Object data = null;
				if(apiInfo == null) {
					data = new HashMap<>(0);
				}else if(ValueType.page == apiInfo.getReturnType()) {
					String body = RequestContextHelper.getCachingBodyString(exchange);
					PageParams pageParam = JsonUtils.toObject(body, PageParams.class);
					data = Page.blankPage(pageParam.getPageNo(), pageParam.getPageSize());
				}else if(ValueType.array == apiInfo.getReturnType()) {
					data = new ArrayList<>(0);
				}else if(ValueType.object == apiInfo.getReturnType()) {
					data = new HashMap<>(0);
				}
				result.put(GlobalConstants.PARAM_DATA, data);
				return result;
			}else if(FallbackStrategy.throwException.name().equals(rule.getStrategy())) {
				throw new MendmixBaseException(MainErrorType.FALLBACK_REQUEST_LIMIT);
			}else if(FallbackStrategy.returnJson.name().equals(rule.getStrategy())) {
				Map<String, Object> result = new HashMap<>(2);
				result.put(GlobalConstants.PARAM_CODE, 200);
				result.put(GlobalConstants.PARAM_DATA, JsonUtils.toHashMap(rule.getFallbackContent()));
				return result;
			}
		} catch (Exception e) {
			if(e instanceof MendmixBaseException) {
				throw e;
			}
			logger.warn(">>handleFallbackBreaker ERROR -> uri:{}, error:{}",exchange.getRequest().getPath().value(),ExceptionFormatUtils.buildExceptionMessages(e, 3));
		}
		return null;
	}
	
	public List<FallbackRule> getAllFallbackRules(){
		List<FallbackRule> list = new ArrayList<>();
		List<BizSystemModule> modules = CurrentSystemHolder.getModules();
		for (BizSystemModule module : modules) {
			list.addAll(getFallbackRules(module.getServiceId(),null));
		}
		return list;
	}

	public List<FallbackRule> getFallbackRules(String serviceId,String uriKey){
		String mappingCacheKey = FALLBACK_RULE_ID_MAPPING_PREFIX + serviceId;
		Collection<String> ruleKeys = CacheUtils.getMapStringValues(mappingCacheKey).values();
		List<FallbackRule> rules = new ArrayList<>(ruleKeys.size());
		FallbackRule rule;
		for (String ruleKey : ruleKeys) {
			rule = CacheUtils.get(ruleKey);
			if(rule != null) {
				if(uriKey == null || uriKey.equals(rule.getUriKey())) {
					rules.add(rule);
				}
			}else {
				CacheUtils.remove(ruleKey);
			}
		}
		return rules;
	}
	
	public String saveFallbackRule(FallbackRule rule) {
		String ruleKey = buildRuleKey(rule);
		if(StringUtils.isBlank(rule.getId())) {
			rule.setId(DigestUtils.md5(ruleKey));
		}
		FallbackRule existsRule = CacheUtils.get(ruleKey);
		if(existsRule != null && !StringUtils.equals(existsRule.getId(), rule.getId())) {
			//throw new MendmixBaseException(String.format("规则[%s:%s]已存在", rule.getHitType(),rule.getUriKey()));
			CacheUtils.remove(ruleKey);
		}
		CacheUtils.set(ruleKey, rule, 0);
		String mappingCacheKey = FALLBACK_RULE_ID_MAPPING_PREFIX + rule.getServiceId();
		CacheUtils.setMapStringValue(mappingCacheKey, rule.getId(), ruleKey);

		String groupMappingKey = buildRuleGroupMappingKey(rule.getServiceId());
		List<String> existsMappings = CacheUtils.getMapValue(groupMappingKey, rule.getUriKey());
		if(existsMappings == null)existsMappings = new ArrayList<>();
		existsMappings.add(ruleKey);
		CacheUtils.setMapValue(groupMappingKey, rule.getUriKey(), existsMappings);
		if(ANY_URI_WILDCARD.equals(rule.getUriKey())) {
			level1Cache.invalidateAll();
		}else {
			level1Cache.invalidate(groupMappingKey + rule.getUriKey());
		}
		logger.info(">> saveFallbackRule finished!!! serviceId:{},rule:{}",rule.getServiceId(),JsonUtils.toJson(rule));
		return rule.getId();
	}
	
	public boolean removeFallbackRule(FallbackRule rule) {
		
		String ruleKey = null;
		if(StringUtils.isNotBlank(rule.getId())) {
			String mappingCacheKey = FALLBACK_RULE_ID_MAPPING_PREFIX + rule.getServiceId();
			ruleKey = CacheUtils.getMapStringValue(mappingCacheKey, rule.getId());
		}
		if(ruleKey == null) {
			ruleKey = buildRuleKey(rule);
		}
		if(!CacheUtils.exists(ruleKey))return false;
		
		CacheUtils.remove(ruleKey);
   
		String groupMappingKey = buildRuleGroupMappingKey(rule.getServiceId());
		List<String> existsMappings = CacheUtils.getMapValue(groupMappingKey, rule.getUriKey());
		if(existsMappings != null && existsMappings.contains(ruleKey)) {
			existsMappings.remove(ruleKey);
			CacheUtils.setMapValue(groupMappingKey, rule.getUriKey(), existsMappings);
		}
		//
		if(ANY_URI_WILDCARD.equals(rule.getUriKey())) {
			level1Cache.invalidateAll();
		}else {
			level1Cache.invalidate(groupMappingKey + rule.getUriKey());
		}
		logger.info(">> removeFallbackRule finished!!! rule:{}",rule);
		
		return true;
	}
	
	public boolean withHitCacheFallbackRule(ServerWebExchange exchange,ApiInfo apiInfo) {
		BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		List<FallbackRule> rules = matchFallbackRules(exchange, module.getServiceId(), apiInfo);
		return rules != null && rules.stream().anyMatch(rule -> FallbackStrategy.hitCache.name().equals(rule.getStrategy()));
	}
	
	private List<FallbackRule> matchFallbackRules(ServerWebExchange exchange,String serviceId,ApiInfo apiInfo) {
		List<FallbackRule> rules = exchange.getAttribute(CTX_FALLBACK_RULE_KEY);
		if(rules != null)return rules;
		BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
		String uriKey;
		if(apiInfo != null) {
			uriKey = apiInfo.getIdentifier();
		}else {
			String method = exchange.getRequest().getMethod().name();
			String uri = RequestContextHelper.getOriginRequestUri(exchange);
			uri = uri.substring(module.getUriPrefix().length());
			uriKey = BizSystemModule.buildApiIdentifier(method, uri);
		}
		
		String groupMappingKey = buildRuleGroupMappingKey(serviceId);
		String localCacheKey = groupMappingKey + uriKey;
		rules = getRuleFromLocalCache(localCacheKey);
		if(rules != null) {
			exchange.getAttributes().put(CTX_FALLBACK_RULE_KEY, rules);
			return rules;
		}
		//
		rules = getFallbackRules(serviceId, uriKey);
		//静态规则
		if((rules == null || rules.isEmpty()) && staticRules.containsKey(module.getServiceId())) {
		   rules = staticRules.get(module.getServiceId()).get(uriKey);
		}
		if(rules == null) {
			rules = blankRules;
			level1Cache.put(localCacheKey, new ExpireableObject(rules, 10));
		}else {
			level1Cache.put(localCacheKey, rules);
		}
		exchange.getAttributes().put(CTX_FALLBACK_RULE_KEY, rules);
		return rules;
	}
	
	@SuppressWarnings("unchecked")
	private List<FallbackRule> getRuleFromLocalCache(String key) {
		Object o = level1Cache.getIfPresent(key);
		if(o instanceof ExpireableObject) {
			ExpireableObject expObj = (ExpireableObject) o;
			return expObj.isExpired() ? null : (List<FallbackRule>)expObj.getTarget();
		}
		return (List<FallbackRule>) o;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<FallbackRule> list = ResourceUtils.getConfigObjects("mendmix-cloud.governance.fallback.rule", FallbackRule.class);
		Map<String, List<FallbackRule>> ruleMaping;
		for (FallbackRule rule : list) {
			ruleMaping = staticRules.get(rule.getServiceId());
			if(ruleMaping == null) {
				staticRules.put(rule.getServiceId(), ruleMaping = new HashMap<>());
			}
			List<FallbackRule> rules = ruleMaping.get(rule.getUriKey());
			if(rules == null) {
				ruleMaping.put(rule.getUriKey(), rules = new ArrayList<>());
			}
			rules.add(rule);
		}
		logger.info(">>>>staticRules>>>>\n{}",JsonUtils.toJson(staticRules));
	}
	
	@Override
	public int order() {
		return 0;
	}
	
	private static String buildRuleGroupMappingKey(String serviceId) {
		return new StringBuilder(URI_REL_FALLBACK_RULE_MAPPING_KEY_PRIFIX)
			     .append(serviceId)
	             .toString();
	}
	
	private static String buildRuleKey(FallbackRule rule) {
		if(StringUtils.isAnyBlank(rule.getServiceId(),rule.getHitType(),rule.getUriKey())) {
			throw new MendmixBaseException("参数[serviceId,hitType,uriKey]不能为空");
		}
		return new StringBuilder(FALLBACK_RULE_KEY_PRIFIX)
				 .append(rule.getServiceId())
				 .append(GlobalConstants.UNDER_LINE)
				 .append(rule.getHitType())
				 .append(GlobalConstants.UNDER_LINE)
			     .append(rule.getUriKey())
	             .toString();
	}

}
