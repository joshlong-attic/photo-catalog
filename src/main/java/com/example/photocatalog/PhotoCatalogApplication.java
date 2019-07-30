package com.example.photocatalog;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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


@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "catalog")
class CatalogProperties {
	private File sourceDirectory;
	private File stagingDirectory;
}

@Log4j2
@Configuration
@RequiredArgsConstructor
class ExpandZipsIfRequiredStep {

	private final FileService fileService;
	private final JdbcTemplate jdbcTemplate;
	private final StepBuilderFactory stepFactory;
	private final TransactionTemplate transactionTemplate;
	private final CatalogProperties catalogProperties;

	@Bean
	Step step() {
		return this.stepFactory
			.get("step1")
			.<Path, File>chunk(1000)
			.reader(this.itemReader())
			.processor((Function<Path, File>) Path::toFile)
			.writer(this.itemWriter())
			.build();
	}

	@Bean
	@SneakyThrows
	ItemReader<Path> itemReader() {
		var stream = Files
			.walk(this.catalogProperties.getSourceDirectory().toPath());
		return new IteratorItemReader<>(stream.iterator());
	}


	@Bean
	ItemWriter<File> itemWriter() {
		return items -> transactionTemplate.execute((TransactionCallback<ItemWriter<File>>) transactionStatus -> {
			items.forEach(fileService::save);
			return null;
		});
	}
}

@Service
@RequiredArgsConstructor
class FileService {

	private final JdbcTemplate jdbcTemplate;
	/*INSERT INTO table_name(column_list) VALUES(value_list)

*/
	private final String insertFileSql = "insert into files ( name, path, ext, exists, size_in_bytes, last_modified ) values(?,  ?, ?, ?, ?, ?) ON CONFLICT  ON CONSTRAINT path_constraint  do nothing   ";
//	private final String insertFileSql = "insert into files ( name, path, ext, exists, size_in_bytes, last_modified )  values( ?,  ?, ?, ?, ?, ? ) ";

	public Number save(File file) {
		var psc = new PreparedStatementCreatorFactory(this.insertFileSql,
			Types.VARCHAR,
			Types.VARCHAR,
			Types.VARCHAR,
			Types.BOOLEAN,
			Types.BIGINT,
			Types.DATE
		);
		psc.setGeneratedKeysColumnNames("id");
		psc.setReturnGeneratedKeys(true);
		var params = new Object[]{
			file.getName(),
			file.getAbsolutePath(),
			FileUtils.getExtensionFor(file),
			file.exists(),
			file.length(),
			new java.sql.Date(file.lastModified()),
		};
		var kh = new GeneratedKeyHolder();
		this.jdbcTemplate.update(psc.newPreparedStatementCreator(params), kh);
		return kh.getKey();
	}
}


abstract class FileUtils {

	static String getExtensionFor(File file) {
		if (file.isDirectory() || file.isHidden()) {
			return null;
		}
		if (file.isFile()) {
			var rindex = file.getName().lastIndexOf(".");
			if (rindex != -1) {
				var ext = file.getName().substring(rindex);
				if (ext.startsWith(".")) {
					ext = ext.substring(1);
				}
				return ext;
			}
		}
		return null;
	}
}

@Configuration
@Log4j2
@RequiredArgsConstructor
class PhotoCatalogJobConfiguration {

	private final JobBuilderFactory jobFactory;
	private final ExpandZipsIfRequiredStep step1;

	@Bean
	Job job() {
		return this.jobFactory
			.get("photo-catalog-job-" + UUID.randomUUID().toString())
			.start(this.step1.step())
			.incrementer(new RunIdIncrementer())
			.build();
	}
}
