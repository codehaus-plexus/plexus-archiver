package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.codehaus.plexus.components.io.fileselectors.FileSelector;

final class FileSelectors {

	private FileSelectors() {
	}
	
	static FileSelector from(
	        Path directory,
	        PathMatcher matcher) {

	    return fileInfo -> matcher.matches(
	            Path.of(fileInfo.getName()));
	}
}
