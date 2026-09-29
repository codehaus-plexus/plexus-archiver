package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;

import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

final class DefaultDirectoryResourceScan implements ResourceScan.DirectoryScan {

	private final Path directory;
	private PathMatcher matcher;

	DefaultDirectoryResourceScan(Path directory) {
		this.directory = Objects.requireNonNull(directory, "directory");
	}

	@Override
	public DefaultDirectoryResourceScan matcher(PathMatcher matcher) {
		this.matcher = Objects.requireNonNull(matcher, "matcher");
		return this;
	}

	@Override
	public Path directory() {
		return directory;
	}

	PathMatcher matcher() {
		return matcher;
	}

	DefaultFileSet toFileSet() {
		DefaultFileSet fileSet = new DefaultFileSet(directory.toFile());

		if (matcher instanceof DefaultPathPatternMatcher resourceMatcher) {
			resourceMatcher.applyTo(fileSet);
		} else {
			fileSet.setFileSelectors(new FileSelector[] { FileSelectors.from(directory, matcher) });
		}

		return fileSet;
	}

}
