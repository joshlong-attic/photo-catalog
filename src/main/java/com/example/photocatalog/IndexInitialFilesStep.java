package com.example.photocatalog;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@Log4j2
@Configuration
@RequiredArgsConstructor
class IndexInitialFilesStep {

	private final JdbcTemplate template;

	private final FileService fileService;

	private final StepBuilderFactory stepFactory;

	private final TransactionTemplate transactionTemplate;

	private final CatalogProperties catalogProperties;

	@Bean
	Step initialFilesIngestStep() {
		return this.stepFactory.get("initialFilesIngestStep")//
				.<Path, File>chunk(1000)//
				.reader(this.itemReader())//
				.processor((Function<Path, File>) Path::toFile)//
				.writer(this.itemWriter())//
				.build();
	}

	@Bean
	@SneakyThrows
	ItemReader<Path> itemReader() {
		var stream = Files.walk(this.catalogProperties.getSourceDirectory().toPath());
		return new IteratorItemReader<>(stream.iterator());
	}

	@Bean
	ItemWriter<File> itemWriter() {
		return items -> this.transactionTemplate.execute((TransactionCallback<ItemWriter<File>>) transactionStatus -> {
			items.stream().map(fileService::save).forEach(id -> {
				log.debug("the id of the save result is " + id);
				this.template.update(" INSERT INTO initial_ingest_files(file_id) VALUES (?) "
						+ " ON CONFLICT ON CONSTRAINT file_id_constraint DO NOTHING", id);
			});
			return null;
		});
	}

}
