/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.codehaus.plexus.archiver.manager;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

import static java.util.Objects.requireNonNull;

abstract class AbstractArchiverManager implements ArchiverManager {

    private final Map<String, ArchiverFactory> archivers;

    private final Map<String, UnArchiverFactory> unArchivers;

    private final Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections;

    protected AbstractArchiverManager(
            Map<String, ArchiverFactory> archivers,
            Map<String, UnArchiverFactory> unArchivers,
            Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections) {
        this.archivers = Collections.unmodifiableMap(archivers);
        this.unArchivers = Collections.unmodifiableMap(unArchivers);
        this.plexusIoResourceCollections = Collections.unmodifiableMap(plexusIoResourceCollections);
    }

    @Override
    @Nonnull
    public final Archiver getArchiver(@Nonnull String archiverName) throws NoSuchArchiverException {
        return getArchiverFactory(archiverName).create();
    }

    @Override
    @Nonnull
    public final ArchiverFactory getArchiverFactory(@Nonnull String archiverName) throws NoSuchArchiverException {
        requireNonNull(archiverName);
        ArchiverFactory archiver = archivers.get(archiverName);
        if (archiver == null) {
            throw new NoSuchArchiverException(archiverName);
        }
        return archiver;
    }

    @Override
    @Nonnull
    public final UnArchiver getUnArchiver(@Nonnull String unArchiverName) throws NoSuchArchiverException {
        return getUnArchiverFactory(unArchiverName).create();
    }

    @Override
    @Nonnull
    public final UnArchiverFactory getUnArchiverFactory(@Nonnull String unArchiverName) throws NoSuchArchiverException {
        requireNonNull(unArchiverName);
        UnArchiverFactory unArchiver = unArchivers.get(unArchiverName);
        if (unArchiver == null) {
            throw new NoSuchArchiverException(unArchiverName);
        }
        return unArchiver;
    }

    @Override
    @Nonnull
    public final PlexusIoResourceCollection getResourceCollection(String resourceCollectionName)
            throws NoSuchArchiverException {
        return getResourceCollectionFactory(resourceCollectionName).create();
    }

    @Override
    @Nonnull
    public final PlexusIoResourceCollectionFactory getResourceCollectionFactory(String resourceCollectionName)
            throws NoSuchArchiverException {
        requireNonNull(resourceCollectionName);
        PlexusIoResourceCollectionFactory resourceCollection = plexusIoResourceCollections.get(resourceCollectionName);
        if (resourceCollection == null) {
            throw new NoSuchArchiverException(resourceCollectionName);
        }
        return resourceCollection;
    }

    @Override
    @Nonnull
    public final Archiver getArchiver(@Nonnull File file) throws NoSuchArchiverException {
        return getArchiver(FileNames.getFileExtension(file));
    }

    @Override
    public Collection<String> getAvailableArchivers() {
        return archivers.keySet();
    }

    @Override
    @Nonnull
    public final UnArchiver getUnArchiver(@Nonnull File file) throws NoSuchArchiverException {
        return getUnArchiver(FileNames.getFileExtension(file));
    }

    @Nonnull
    @Override
    public final Collection<String> getAvailableUnArchivers() {
        return unArchivers.keySet();
    }

    @Override
    @Nonnull
    public final PlexusIoResourceCollection getResourceCollection(@Nonnull File file) throws NoSuchArchiverException {
        return getResourceCollection(FileNames.getFileExtension(file));
    }

    @Nonnull
    @Override
    public final Collection<String> getAvailableResourceCollections() {
        return plexusIoResourceCollections.keySet();
    }
}
