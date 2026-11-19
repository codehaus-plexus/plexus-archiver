package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.List;

import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

public interface UnArchiverSettings {
	
	static UnArchiverSettings of(UnArchiver unArchiver) {
		return new DefaultUnArchiverSettings(unArchiver);
	}
	
    void setSource(Path source);

    void setDestinationDirectory(Path destinationDirectory);

    void setDestinationFile(Path destinationFile);

    void setExistingFileHandling(ExistingFileHandling existingFileHandling);

    void setFileMappers(List<FileMapper> fileMappers);

    void setFileSelectors(List<FileSelector> fileSelectors);

    void setPermissionHandling(PermissionHandling permissionHandling);
}
