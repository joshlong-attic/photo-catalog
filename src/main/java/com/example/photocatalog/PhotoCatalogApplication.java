package com.example.photocatalog;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Log4j2
@EnableBatchProcessing
@EnableConfigurationProperties(CatalogProperties.class)
@SpringBootApplication
public class PhotoCatalogApplication {

	@Bean
	TransactionTemplate transactionTemplate(PlatformTransactionManager txm) {
		return new TransactionTemplate(txm);
	}

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	public static void main(String[] args) {
		SpringApplication.run(PhotoCatalogApplication.class, args);
	}

}
