package com.example.photocatalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "catalog")
class CatalogProperties {

	private File sourceDirectory;

	private File expandedDirectory;

	private File stagingDirectory;

}
