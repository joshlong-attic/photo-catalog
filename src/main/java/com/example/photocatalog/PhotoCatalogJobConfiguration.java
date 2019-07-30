package com.example.photocatalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@Log4j2
@RequiredArgsConstructor
class PhotoCatalogJobConfiguration {

	private final JobBuilderFactory jobFactory;

	private final IndexInitialFilesStep step1;

	private final ExtractZipFilesStep step2;

	@Bean
	Job job() {
		return this.jobFactory//
				.get("photo-catalog-job-" + UUID.randomUUID().toString())//
				.start(this.step1.initialFilesIngestStep())//
				.next(this.step2.extractZipsStep())//
				.incrementer(new RunIdIncrementer())//
				.build();
	}

}
