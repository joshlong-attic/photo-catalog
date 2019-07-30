package com.example.photocatalog;

import java.io.File;

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
				return ext.toLowerCase();
			}
		}
		return null;
	}

}
