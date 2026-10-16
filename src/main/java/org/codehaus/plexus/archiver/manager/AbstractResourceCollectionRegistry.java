package org.codehaus.plexus.archiver.manager;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.Map;

import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

import static java.util.Objects.requireNonNull;

abstract class AbstractResourceCollectionRegistry implements ResourceCollectionRegistry {

    private final Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections;

    protected AbstractResourceCollectionRegistry(
            Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections) {
        this.plexusIoResourceCollections = plexusIoResourceCollections;
    }

    @Override
    public final PlexusIoResourceCollection getResourceCollection(@Nonnull File file) throws NoSuchArchiverException {
        return getResourceCollection(FileNames.getFileExtension(file));
    }

    public final PlexusIoResourceCollection getResourceCollection(String resourceCollectionName)
            throws NoSuchArchiverException {
        return getResourceCollectionFactory(resourceCollectionName).create();
    }

    public final PlexusIoResourceCollectionFactory getResourceCollectionFactory(String resourceCollectionName)
            throws NoSuchArchiverException {
        requireNonNull(resourceCollectionName);
        PlexusIoResourceCollectionFactory resourceCollection = plexusIoResourceCollections.get(resourceCollectionName);
        if (resourceCollection == null) {
            throw new NoSuchArchiverException(resourceCollectionName);
        }
        return resourceCollection;
    }
}
