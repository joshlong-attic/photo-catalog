package com.example.photocatalog;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@EnableBatchProcessing
@Log4j2
@SpringBootApplication
public class PhotoCatalogApplication {

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	public static void main(String[] args) {
		SpringApplication.run(PhotoCatalogApplication.class, args);
	}
}


@Configuration
@Log4j2
@RequiredArgsConstructor
class PhotoCatalogJobConfiguration {

	private final StepBuilderFactory stepBuilderFactory;
	private final JobBuilderFactory jobBuilderFactory;

	private final ItemWriter<File> itemWriter = list -> {
		list.forEach(log::info);
	};

	@Bean
	@SneakyThrows
	ItemReader<Path> itemReader() {
		var file = new File("/home/jlong/Downloads/photo-catalog");
		var stream = Files.walk(file.toPath());
		return new IteratorItemReader<>(stream.iterator());
	}

	@Bean
	Step step() {
		return this.stepBuilderFactory
			.get("step1")
			.<Path, File>chunk(100)
			.reader(this.itemReader())
			.processor((Function<Path, File>) Path::toFile)
			.writer(this.itemWriter)
			.build();
	}

	@Bean
	Job job() {
		return this.jobBuilderFactory
			.get("photo-catalog-job-" + UUID.randomUUID().toString())
			.start(this.step())
			.incrementer(new RunIdIncrementer())
			.build();
	}
}
