package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.Objects;

import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;

final class DefaultArchiveResourceScan implements ResourceScan.ArchiveScan {

	private final Path archive;
	private ArchiveEntryMatcher matcher;

	DefaultArchiveResourceScan(Path archive) {
		this.archive = Objects.requireNonNull(archive, "archive");
	}

	@Override
	public DefaultArchiveResourceScan matcher(ArchiveEntryMatcher matcher) {
		this.matcher = Objects.requireNonNull(matcher, "matcher");
		return this;
	}

	@Override
	public Path archive() {
		return archive;
	}

	ArchiveEntryMatcher matcher() {
		return matcher;
	}

	DefaultArchivedFileSet toArchivedFileSet() {
		DefaultArchivedFileSet fileSet = new DefaultArchivedFileSet(archive.toFile());

		if (matcher instanceof DefaultPathPatternMatcher resourceMatcher) {
			resourceMatcher.applyTo(fileSet);
		} else {
			throw new UnsupportedOperationException();
		}

		return fileSet;
	}
}