package com.jeesuite.security;

import java.io.Serializable;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.cache.LocalCache;
import com.jeesuite.security.cache.RedisCache;
import com.jeesuite.security.model.AccessToken;
import com.jeesuite.security.model.BaseUserInfo;
import com.jeesuite.security.util.SecurityCryptUtils;

public class SecurityOauth2Manager {

	private static final Integer TOKEN_EXPIRED_SECONDS = ResourceUtils.getInt("security.oauth2.access-token.expirein", 3600 * 24);
	private Cache cache;
	private Cache tokenCache;
	
	public SecurityOauth2Manager(SecurityDecisionProvider decisionProvider) {
	       if(CacheType.redis == decisionProvider.cacheType()){
	    	   this.cache = new RedisCache("security:oauth2:authCode", 180);
	    	   this.tokenCache = new RedisCache("security:oauth2:token", TOKEN_EXPIRED_SECONDS);
			}else{
				this.cache = new LocalCache(180);
				this.tokenCache = new RedisCache("security:oauth2:token", TOKEN_EXPIRED_SECONDS);
			}
	}
	
	public String createOauth2AuthCode(Serializable userId){
		String authCode = SecurityCryptUtils.generateAuthCode();
		cache.setString(authCode, userId.toString());
		return authCode;
	}
	
	public String authCode2UserId(String authCode){
		return cache.getString(authCode);
	}
	
	public AccessToken createAccessToken(BaseUserInfo user){
		AccessToken accessToken = new AccessToken();
		accessToken.setAccess_token(TokenGenerator.generate());
		accessToken.setRefresh_token(TokenGenerator.generate());
		accessToken.setExpires_in(TOKEN_EXPIRED_SECONDS);
		tokenCache.setObject(accessToken.getAccess_token(), accessToken);
		return accessToken;
	}
}
