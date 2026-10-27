package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;

public interface ArchiverSettings {

	static ArchiverSettings of(Archiver archiver) {
		return new DefaultArchiverSettings(archiver);
	}
	
	void addResource(
            ResourceScan scan,
            Consumer<ResourceEmitConfigurer> emit);
	
    void setDestFile(Path destFile);

    void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling);

    void setDotFileDirectory(Path dotFileDirectory);

    void setForced(ArchiveCreation archiveCreation);

    void setDuplicateBehavior(DuplicateHandling duplicateHandling);

    void setPermissionPolicy(PermissionPolicy policy);
}
