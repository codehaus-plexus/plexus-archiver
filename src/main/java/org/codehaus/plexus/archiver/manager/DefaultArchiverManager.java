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

import java.io.File;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import static java.util.Objects.requireNonNull;

/**
 * @author dantran
 */
@Singleton
@Named
public class DefaultArchiverManager
    implements ArchiverManager
{

    private final Map<String, Provider<Archiver>> archivers;

    private final Map<String, Provider<UnArchiver>> unArchivers;

    private final Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections;

    @Inject
    public DefaultArchiverManager( Map<String, Provider<Archiver>> archivers,
                                   Map<String, Provider<UnArchiver>> unArchivers,
                                   Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections )
    {
        this.archivers = requireNonNull( archivers );
        this.unArchivers = requireNonNull( unArchivers );
        this.plexusIoResourceCollections = requireNonNull( plexusIoResourceCollections );
    }

    @Override
    @Nonnull public Archiver getArchiver( @Nonnull String archiverName )
        throws NoSuchArchiverException
    {
        requireNonNull( archiverName );
        Provider<Archiver> archiver = archivers.get( archiverName );
        if ( archiver == null )
        {
            throw new NoSuchArchiverException( archiverName );
        }
        return archiver.get();
    }

    @Override
    @Nonnull public UnArchiver getUnArchiver( @Nonnull String unArchiverName )
        throws NoSuchArchiverException
    {
        requireNonNull( unArchiverName );
        Provider<UnArchiver> unArchiver = unArchivers.get( unArchiverName );
        if ( unArchiver == null )
        {
            throw new NoSuchArchiverException( unArchiverName );
        }
        return unArchiver.get();
    }

    @Override
    public @Nonnull
    PlexusIoResourceCollection getResourceCollection( String resourceCollectionName )
        throws NoSuchArchiverException
    {
        requireNonNull( resourceCollectionName );
        Provider<PlexusIoResourceCollection> resourceCollection =
                plexusIoResourceCollections.get( resourceCollectionName );
        if ( resourceCollection == null )
        {
            throw new NoSuchArchiverException( resourceCollectionName );
        }
        return resourceCollection.get();
    }

    private static @Nonnull
    String getFileExtention( @Nonnull File file )
    {
        String path = file.getAbsolutePath();

        String archiveExt = FileUtils.getExtension( path ).toLowerCase( Locale.ENGLISH );

        if ( "gz".equals( archiveExt )
                 || "bz2".equals( archiveExt )
                 || "xz".equals( archiveExt )
                 || "zst".equals( archiveExt )
                 || "snappy".equals( archiveExt ) )
        {
            String[] tokens = StringUtils.split( path, "." );

            if ( tokens.length > 2 && "tar".equals( tokens[tokens.length - 2].toLowerCase( Locale.ENGLISH ) ) )
            {
                archiveExt = "tar." + archiveExt;
            }
        }

        return archiveExt;

    }

    @Override
    @Nonnull public Archiver getArchiver( @Nonnull File file )
        throws NoSuchArchiverException
    {
        return getArchiver( getFileExtention( file ) );
    }

    @Override
    @Nonnull public UnArchiver getUnArchiver( @Nonnull File file )
        throws NoSuchArchiverException
    {
        return getUnArchiver( getFileExtention( file ) );
    }

    @Override
    @Nonnull public PlexusIoResourceCollection getResourceCollection( @Nonnull File file )
        throws NoSuchArchiverException
    {
        return getResourceCollection( getFileExtention( file ) );
    }

}
