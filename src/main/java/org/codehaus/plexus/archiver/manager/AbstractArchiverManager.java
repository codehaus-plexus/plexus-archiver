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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.util.StringUtils;

import static java.util.Objects.requireNonNull;

/**
 * Shared lookup logic of the archiver managers.
 * <p>
 * On the 4.x line the public methods stay non-final: up to 4.15.0 they were declared on
 * {@link DefaultArchiverManager}, which consumers subclass and override. Being non-final also lets
 * javac emit public bridge methods in the public subclasses, so reflective calls through
 * {@code DefaultArchiverManager} do not reach this package-private class.
 */
abstract class AbstractArchiverManager implements ArchiverManager {

    private final Map<String, Supplier<Archiver>> archivers;

    private final Map<String, Supplier<UnArchiver>> unArchivers;

    private final Map<String, Supplier<PlexusIoResourceCollection>> plexusIoResourceCollections;

    protected AbstractArchiverManager(
            Map<String, Supplier<Archiver>> archivers,
            Map<String, Supplier<UnArchiver>> unArchivers,
            Map<String, Supplier<PlexusIoResourceCollection>> plexusIoResourceCollections) {
        this.archivers = Collections.unmodifiableMap(new HashMap<>(archivers));
        this.unArchivers = Collections.unmodifiableMap(new HashMap<>(unArchivers));
        this.plexusIoResourceCollections = Collections.unmodifiableMap(new HashMap<>(plexusIoResourceCollections));
    }

    @Override
    @Nonnull
    public Archiver getArchiver(@Nonnull String archiverName) throws NoSuchArchiverException {
        requireNonNull(archiverName);
        Supplier<Archiver> archiver = archivers.get(archiverName);
        if (archiver == null) {
            throw new NoSuchArchiverException(archiverName);
        }
        return archiver.get();
    }

    @Override
    @Nonnull
    public UnArchiver getUnArchiver(@Nonnull String unArchiverName) throws NoSuchArchiverException {
        requireNonNull(unArchiverName);
        Supplier<UnArchiver> unArchiver = unArchivers.get(unArchiverName);
        if (unArchiver == null) {
            throw new NoSuchArchiverException(unArchiverName);
        }
        return unArchiver.get();
    }

    @Override
    @Nonnull
    public PlexusIoResourceCollection getResourceCollection(String resourceCollectionName)
            throws NoSuchArchiverException {
        requireNonNull(resourceCollectionName);
        Supplier<PlexusIoResourceCollection> resourceCollection =
                plexusIoResourceCollections.get(resourceCollectionName);
        if (resourceCollection == null) {
            throw new NoSuchArchiverException(resourceCollectionName);
        }
        return resourceCollection.get();
    }

    @Override
    @Nonnull
    public Archiver getArchiver(@Nonnull File file) throws NoSuchArchiverException {
        return getArchiver(getFileExtension(file));
    }

    @Override
    public Collection<String> getAvailableArchivers() {
        return archivers.keySet();
    }

    @Override
    @Nonnull
    public UnArchiver getUnArchiver(@Nonnull File file) throws NoSuchArchiverException {
        return getUnArchiver(getFileExtension(file));
    }

    @Nonnull
    @Override
    public Collection<String> getAvailableUnArchivers() {
        return unArchivers.keySet();
    }

    @Override
    @Nonnull
    public PlexusIoResourceCollection getResourceCollection(@Nonnull File file) throws NoSuchArchiverException {
        return getResourceCollection(getFileExtension(file));
    }

    @Nonnull
    @Override
    public Collection<String> getAvailableResourceCollections() {
        return plexusIoResourceCollections.keySet();
    }

    @Nonnull
    private static String getFileExtension(@Nonnull File file) {

        String fileName = file.getName().toLowerCase(Locale.ROOT);
        String[] tokens = StringUtils.split(fileName, ".");

        String archiveExt = "";

        if (tokens.length == 2) {
            archiveExt = tokens[1];
        } else if (tokens.length > 2 && "tar".equals(tokens[tokens.length - 2])) {
            archiveExt = "tar." + tokens[tokens.length - 1];
        } else if (tokens.length > 2) {
            archiveExt = tokens[tokens.length - 1];
        }

        return archiveExt;
    }
}
