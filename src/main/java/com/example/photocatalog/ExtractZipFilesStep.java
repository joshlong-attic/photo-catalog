package com.example.photocatalog;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;

@Configuration
@Log4j2
@RequiredArgsConstructor
class ExtractZipFilesStep {

	private final FileService fileService;

	private final TransactionTemplate transactionTemplate;

	private final JdbcTemplate jdbcTemplate;

	private final StepBuilderFactory stepBuilderFactory;

	private final CatalogProperties catalogProperties;

	private final DataSource dataSource;

	@Bean
	ItemReader<File> zipFileItemReader() {
		return new JdbcCursorItemReaderBuilder<File>().dataSource(this.dataSource).name("zip_file_reader")
				.rowMapper((rs, rowNum) -> new File(rs.getString("path")))
				.sql("select * from initial_ingest_files iif, files f  where f.id = iif.file_id and  f.ext = 'zip'")
				.build();
	}

	@Bean
	ItemWriter<File> expandedItemWriter() {
		var expandedDirectory = this.catalogProperties.getExpandedDirectory();
		return items -> {
			transactionTemplate.execute(transactionStatus -> {
				for (var f : items) {

					if (!f.exists())
						continue;

					var dest = new File(expandedDirectory,
							f.getName().toLowerCase().replace(" ", "_").replace(".zip", ""));
					log.debug("unzipping " + f.getAbsolutePath() + " to " + dest.getAbsolutePath() + '.');
					Assert.isTrue(dest.exists() || dest.mkdirs(), "the directory to which you're writing must exist.");
					unzip(f, dest);
					var savedId = this.fileService.save(dest);
					this.jdbcTemplate.update(
							" INSERT INTO expanded_folders(file_id) VALUES (?) ON CONFLICT ON CONSTRAINT ef_file_id_constraint DO NOTHING",
							savedId);
				}
				return null;
			});
		};
	}

	@SneakyThrows
	private void unzip(File f, File dest) {
		UnzipUtils.unzip(f, dest);
	}

	@Bean
	Step extractZipsStep() {
		return this.stepBuilderFactory.get("extract-zip-file").<File, File>chunk(5).reader(this.zipFileItemReader())
				.writer(this.expandedItemWriter()).build();

	}

}
