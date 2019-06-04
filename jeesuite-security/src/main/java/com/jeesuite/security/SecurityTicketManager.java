package com.jeesuite.security;

import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.security.SecurityConstants.CacheType;
import com.jeesuite.security.cache.LocalCache;
import com.jeesuite.security.cache.RedisCache;

public class SecurityTicketManager {

	private Cache cache;
	
	public SecurityTicketManager(SecurityDecisionProvider decisionProvider) {
	       if(CacheType.redis == decisionProvider.cacheType()){
	    	   this.cache = new RedisCache("security.ticket:", 180);
			}else{
				this.cache = new LocalCache(180);
			}
	}
	
	public String setTicketObject(Object ticketObject){
		String ticket = TokenGenerator.generate();
		cache.setObject(ticket, ticketObject);
		return ticket;
	}
	
	public <T> T getTicketObject(String ticket){
		return cache.getObject(ticket);
	}
	
}
