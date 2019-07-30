package com.example.photocatalog;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class UnzipUtils {

	@SneakyThrows
	public static Collection<File> unzip(File zipfile, File targetDirectory) {
		var list = new ArrayList<File>();
		var buffer = new byte[1024];
		try (var zis = new ZipInputStream(new FileInputStream(zipfile))) {
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				var newFile = newFile(targetDirectory, zipEntry);
				if (zipEntry.isDirectory()) {
					log.info("zip entry is a directory: " + zipEntry.getName());
					log.info("new destination directory is " + newFile.getParentFile().getAbsolutePath());
					Assert.isTrue(newFile.exists() || newFile.mkdirs(), "the directory must exist");
				}
				else {

					Assert.isTrue(newFile.getParentFile().exists() || newFile.getParentFile().mkdirs(),
							"the parent directory " + newFile.getParentFile().getAbsolutePath() + " must exist.");
					boolean already = (newFile.exists());
					if (!already) {
						/*
						 * newFile.delete(); } else {
						 */
						var tmpExpandedFile = Files.createTempFile("expanded", newFile.getName());
						try (var fos = new BufferedOutputStream(new FileOutputStream(tmpExpandedFile.toFile()))) {
							var len = 0;
							while ((len = zis.read(buffer)) > 0) {
								fos.write(buffer, 0, len);
							}
						}
						Assert.isTrue(newFile.getParentFile().exists() || newFile.getParentFile().mkdirs(),
								"the directory to which you're writing must exist");
						Files.move(tmpExpandedFile, newFile.toPath());
					}

					list.add(newFile);
				}
			}
			zis.closeEntry();
		}
		return list;
	}

	@SneakyThrows
	private static File newFile(File destinationDir, ZipEntry zipEntry) {
		var destFile = new File(destinationDir, zipEntry.getName());
		var destDirPath = destinationDir.getCanonicalPath();
		var destFilePath = destFile.getCanonicalPath();
		var entryIsInTargetDirectory = destFilePath.startsWith(destDirPath + File.separator);
		Assert.isTrue(entryIsInTargetDirectory, "Entry is outside of the target dir: " + zipEntry.getName());
		return destFile;
	}

}