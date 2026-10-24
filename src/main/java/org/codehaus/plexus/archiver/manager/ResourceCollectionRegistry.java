package org.codehaus.plexus.archiver.manager;

import java.io.File;

import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

public interface ResourceCollectionRegistry {

    PlexusIoResourceCollection getResourceCollection(File archiveFile) throws NoSuchArchiverException;
}
