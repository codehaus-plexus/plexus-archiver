package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

public sealed interface ResourceScan {
	
	static DirectoryScan fromDirectory(Path directory) {
		return new DefaultDirectoryResourceScan(directory);
	}
	
	static ArchiveScan fromArchive(Path archive) {
		return new DefaultArchiveResourceScan(archive);
	}
	
	sealed interface DirectoryScan extends ResourceScan permits DefaultDirectoryResourceScan {
		Path directory();
		
		DirectoryScan matcher(PathMatcher matcher);
	}
	
	sealed interface ArchiveScan extends ResourceScan permits DefaultArchiveResourceScan {
		Path archive();

		ArchiveScan matcher(ArchiveEntryMatcher matcher);
	}
}
