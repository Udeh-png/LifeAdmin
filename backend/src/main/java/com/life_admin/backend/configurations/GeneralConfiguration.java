package com.life_admin.backend.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class GeneralConfiguration {
	
	@Bean
	public TransactionTemplate transactionTemplate (MongoTransactionManager manager) {
		return new TransactionTemplate(manager);
	}
	
	@Bean
	MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
		return new MongoTransactionManager(factory);
	}
}
