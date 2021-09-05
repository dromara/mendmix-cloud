package com.jeesuite.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

/**
 * 
 * <br>
 * Class Name   : MultiTenantMongoDbFactory
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月25日
 */
public class MultiTenantMongoDbFactory implements MongoDatabaseFactory,InitializingBean ,DisposableBean{

	private final static Logger logger = LoggerFactory.getLogger("com.zyframework.core.mongodb");

	//
	private Map<String, SimpleMongoClientDatabaseFactory> realTenantFactoryMappings = new HashMap<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		new SimpleMongoClientDatabaseFactory(mongoClient, databaseName)
		//MultiTenantMongoTemplate.addMongoDbFactory(tenantId, mongoDbFactory);
		
	}

	@Override
	public MongoDatabase getMongoDatabase() throws DataAccessException {
		return getCurrentClientDatabaseFactory().getMongoDatabase();
	}

	@Override
	public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
		return getCurrentClientDatabaseFactory().getMongoDatabase(dbName);
	}

	@Override
	public PersistenceExceptionTranslator getExceptionTranslator() {
		return getCurrentClientDatabaseFactory().getExceptionTranslator();
	}

	@Override
	public ClientSession getSession(ClientSessionOptions options) {
		return getCurrentClientDatabaseFactory().getSession(options);
	}

	@Override
	public MongoDatabaseFactory withSession(ClientSession session) {
		return getCurrentClientDatabaseFactory().withSession(session);
	}
	
	@Override
	public void destroy() throws Exception {
		realTenantFactoryMappings.values().forEach(c -> {
			try {
				c.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
private MongoClient buildMongoClient(DataSourceConfig config){
		
	
		Builder builder = new MongoClientOptions.Builder();
		// 连接池设置为300个连接,默认为100
		builder.connectionsPerHost(200);
		// 连接超时毫秒
		builder.connectTimeout(5000);
		//最大等待时间
		builder.maxWaitTime(60000);
		// 套接字超时时间，0无限制
		builder.socketTimeout(10000);
		// 线程队列数
		builder.threadsAllowedToBlockForConnectionMultiplier(10);
		if(StringUtils.isNotBlank(config.getReplicaSetName())){
			builder.requiredReplicaSetName(config.getReplicaSetName());
			builder.readPreference(ReadPreference.secondaryPreferred());//读取偏好, 首先从从节点读取.
			//写关注为1 ,写入主节点即返回.
			builder.writeConcern(WriteConcern.ACKNOWLEDGED);
		}
		MongoClientOptions myOptions = builder.build();
		
		MongoCredential credential = null;
		if(StringUtils.isNoneBlank(config.getUser(),config.getPassword())){			
			credential = MongoCredential.createCredential(config.getUser(), config.getAuthDatabaseName(), config.getPassword().toCharArray());
		}

		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		String[] hostPortArray = config.getServers().split(";");
		for (String hostPorts : hostPortArray) {
			String[] arr = hostPorts.split(":");
			seeds.add(new ServerAddress(arr[0].toString(), Integer.parseInt(arr[1])));
		}
		
		 MongoClient mongoClient;
		 if(credential == null){
			 mongoClient = new MongoClient(seeds, myOptions);
		 }else{
			 mongoClient = new MongoClient(seeds, credential, myOptions);
		 }
		 
		 return mongoClient;
	}
	
	private SimpleMongoClientDatabaseFactory getCurrentClientDatabaseFactory() {
		return realTenantFactoryMappings.get(TenantHolder.getTenantId());
	}
	
}
