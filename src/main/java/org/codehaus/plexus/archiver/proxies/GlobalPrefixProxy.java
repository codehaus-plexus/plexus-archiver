package org.codehaus.plexus.archiver.proxies;
/*
 * Copyright 2014 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.diags.DelegatingArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Adds a global prefix to every element in this archive.
 */
public class GlobalPrefixProxy extends DelegatingArchiver
{
    private final  Archiver archiver;
    private final String globalPrefix;

    public GlobalPrefixProxy( String prefix, Archiver archiver )
    {
        super( archiver);
        if ( !"".equals( prefix ) && !prefix.endsWith( "/" ) )
        {
            this.globalPrefix = prefix  + "/";
        } else {
            this.globalPrefix = prefix;
        }
        this.archiver = archiver;
    }

    @Deprecated
    public void addDirectory( @Nonnull File directory )
        throws ArchiverException
    {
        archiver.addDirectory( directory, globalPrefix );
    }

    @Deprecated
    public void addDirectory( @Nonnull File directory, String prefix )
        throws ArchiverException
    {
        archiver.addDirectory( directory, globalPrefix + prefix );
    }

    @Deprecated
    public void addDirectory( @Nonnull File directory, String[] includes, String[] excludes )
        throws ArchiverException
    {
        archiver.addDirectory( directory, globalPrefix, includes, excludes );
    }

    public void addDirectory( @Nonnull File directory, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        archiver.addDirectory( directory, globalPrefix + prefix, includes, excludes );
    }


    public void addSymlink( String symlinkName, String symlinkDestination )
        throws ArchiverException
    {
        archiver.addSymlink( globalPrefix +  symlinkName, symlinkDestination );
    }

    public void addSymlink( String symlinkName, int permissions, String symlinkDestination )
        throws ArchiverException
    {
        archiver.addSymlink( globalPrefix +  symlinkName, permissions, symlinkDestination );
    }

    public void addFile( @Nonnull File inputFile, @Nonnull String destFileName )
        throws ArchiverException
    {
        archiver.addFile( inputFile,  globalPrefix +  destFileName );
    }

    public void addFile( @Nonnull File inputFile, @Nonnull String destFileName, int permissions )
        throws ArchiverException
    {
        archiver.addFile( inputFile,  globalPrefix +  destFileName, permissions );
    }

    public void addArchivedFileSet( @Nonnull File archiveFile )
        throws ArchiverException
    {
        archiver.addArchivedFileSet( archiveFile, globalPrefix );
    }

    @Deprecated
    public void addArchivedFileSet( @Nonnull File archiveFile, String prefix )
        throws ArchiverException
    {
        archiver.addArchivedFileSet( archiveFile, globalPrefix + prefix );
    }

    public void addArchivedFileSet( File archiveFile, String[] includes, String[] excludes )
        throws ArchiverException
    {
        archiver.addArchivedFileSet( archiveFile, globalPrefix, includes, excludes );
    }

    public void addArchivedFileSet( @Nonnull File archiveFile, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        archiver.addArchivedFileSet( archiveFile, globalPrefix + prefix, includes, excludes );
    }

    public void addArchivedFileSet( final ArchivedFileSet fileSet )
        throws ArchiverException
    {
        ArchivedFileSet copy = new DelegatingArchivedFileSet( fileSet )
        {
            @Override
            public String getPrefix()
            {
                return globalPrefix + super.getPrefix();
            }
        };
        archiver.addArchivedFileSet( copy );
    }

    public void addFileSet( @Nonnull FileSet fileSet )
        throws ArchiverException
    {
        archiver.addFileSet( new DelegatingFileSet( fileSet ){
            @Override
            public String getPrefix()
            {
                return globalPrefix + super.getPrefix();
            }
        } );
    }


    public void addResource( PlexusIoResource resource, String destFileName, int permissions )
        throws ArchiverException
    {
        archiver.addResource( resource, globalPrefix +  destFileName, permissions );
    }

    public void addResources( PlexusIoResourceCollection resources )
        throws ArchiverException
    {
        // todo: Make this work !
        archiver.addResources( resources );
    }
}
