package org.codehaus.plexus.archivers.config;

import org.apache.commons.compress.archivers.ArchiveEntry;

@FunctionalInterface
public interface ArchiveEntryMatcher {

	boolean matches(ArchiveEntry archiveEntry);
}
