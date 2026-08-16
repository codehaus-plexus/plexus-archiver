/*
 * Copyright  2001,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.codehaus.plexus.archiver.manager;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.Collection;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

/**
 * @author dantran
 */
public interface ArchiverManager {
    @Nonnull
    Archiver getArchiver(@Nonnull String archiverName) throws NoSuchArchiverException;

    /**
     * Returns a factory for the named archive format.
     *
     * @since 5.0.0
     */
    @Nonnull
    ArchiverFactory getArchiverFactory(@Nonnull String archiverName) throws NoSuchArchiverException;

    @Nonnull
    Archiver getArchiver(@Nonnull File file) throws NoSuchArchiverException;

    /**
     * Returns an archiver factory selected from the file extension.
     *
     * @since 5.0.0
     */
    @Nonnull
    ArchiverFactory getArchiverFactory(@Nonnull File file) throws NoSuchArchiverException;

    @Nonnull
    Collection<String> getAvailableArchivers();

    @Nonnull
    UnArchiver getUnArchiver(@Nonnull String unArchiverName) throws NoSuchArchiverException;

    /**
     * Returns a factory for the named archive extraction format.
     *
     * @since 5.0.0
     */
    @Nonnull
    UnArchiverFactory getUnArchiverFactory(@Nonnull String unArchiverName) throws NoSuchArchiverException;

    @Nonnull
    UnArchiver getUnArchiver(@Nonnull File file) throws NoSuchArchiverException;

    /**
     * Returns an unarchiver factory selected from the file extension.
     *
     * @since 5.0.0
     */
    @Nonnull
    UnArchiverFactory getUnArchiverFactory(@Nonnull File file) throws NoSuchArchiverException;

    @Nonnull
    Collection<String> getAvailableUnArchivers();

    @Nonnull
    PlexusIoResourceCollection getResourceCollection(@Nonnull File file) throws NoSuchArchiverException;

    /**
     * Returns a resource collection factory selected from the file extension.
     *
     * @since 5.0.0
     */
    @Nonnull
    PlexusIoResourceCollectionFactory getResourceCollectionFactory(@Nonnull File file) throws NoSuchArchiverException;

    @Nonnull
    PlexusIoResourceCollection getResourceCollection(String unArchiverName) throws NoSuchArchiverException;

    /**
     * Returns a resource collection factory for the named format.
     *
     * @since 5.0.0
     */
    @Nonnull
    PlexusIoResourceCollectionFactory getResourceCollectionFactory(String resourceCollectionName)
            throws NoSuchArchiverException;

    @Nonnull
    Collection<String> getAvailableResourceCollections();
}
