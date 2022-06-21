/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.plexus.archiver.diags;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.util.StringUtils;

/**
 * A diagnostic archiver that keeps track of stuff that has been added.
 */
public class TrackingArchiver
    implements Archiver
{

    private File destFile;

    public final List<Addition> added = new ArrayList<>();

    private boolean useJvmChmod;

    private boolean ignorePermissions;

    private FileTime lastModifiedTime;

    private Comparator<String> filenameComparator;

    @Override
    public void createArchive()
        throws ArchiverException, IOException
    {
    }

    @Override
    public void addDirectory( final @Nonnull File directory )
        throws ArchiverException
    {
        added.add( new Addition( directory, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addDirectory( final @Nonnull File directory, final String prefix )
        throws ArchiverException
    {
        added.add( new Addition( directory, prefix, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addDirectory( final @Nonnull File directory, final String[] includes, final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( directory, null, includes, excludes, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addDirectory( final @Nonnull File directory, final String prefix, final String[] includes,
                              final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( directory, prefix, includes, excludes,
                                 PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );

    }

    @Override
    public void addFileSet( final @Nonnull FileSet fileSet )
        throws ArchiverException
    {
        added.add( new Addition( fileSet, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addFile( final @Nonnull File inputFile, final @Nonnull String destFileName )
        throws ArchiverException
    {
        added.add( new Addition( inputFile, destFileName, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addFile( final @Nonnull File inputFile, final @Nonnull String destFileName, final int permissions )
        throws ArchiverException
    {
        added.add( new Addition( inputFile, destFileName, null, null, permissions ) );
    }

    @Override
    public void addArchivedFileSet( final @Nonnull File archiveFile )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addArchivedFileSet( final @Nonnull File archiveFile, final String prefix )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, prefix, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addSymlink( String s, String s2 )
        throws ArchiverException
    {
        added.add( new Addition( s, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addSymlink( String s, int i, String s2 )
        throws ArchiverException
    {
        added.add( new Addition( s, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );

    }

    @Override
    public void addArchivedFileSet( final File archiveFile, final String[] includes, final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, null, includes, excludes,
                                 PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );

    }

    @Override
    public void addArchivedFileSet( final @Nonnull File archiveFile, final String prefix, final String[] includes,
                                    final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, prefix, includes, excludes,
                                 PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );

    }

    @Override
    public void addArchivedFileSet( final ArchivedFileSet fileSet )
        throws ArchiverException
    {
        added.add( new Addition( fileSet, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addArchivedFileSet( final ArchivedFileSet fileSet, Charset charset )
        throws ArchiverException
    {
        added.add( new Addition( fileSet, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public void addResource( final PlexusIoResource resource, final String destFileName, final int permissions )
        throws ArchiverException
    {
        added.add( new Addition( resource, destFileName, null, null, permissions ) );
    }

    @Override
    public void addResources( final PlexusIoResourceCollection resources )
        throws ArchiverException
    {
        added.add( new Addition( resources, null, null, null, PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE ) );
    }

    @Override
    public File getDestFile()
    {
        return destFile;
    }

    @Override
    public void setDestFile( final File destFile )
    {
        this.destFile = destFile;
    }

    @Override
    public void setFileMode( final int mode )
    {
    }

    @Override
    public int getFileMode()
    {
        return Integer.parseInt( "0644", 8 );
    }

    @Override
    public int getOverrideFileMode()
    {
        return Integer.parseInt( "0644", 8 );
    }

    @Override
    public void setDefaultFileMode( final int mode )
    {
    }

    @Override
    public int getDefaultFileMode()
    {
        return Integer.parseInt( "0644", 8 );
    }

    @Override
    public void setDirectoryMode( final int mode )
    {
    }

    @Override
    public int getDirectoryMode()
    {
        return Integer.parseInt( "0755", 8 );
    }

    @Override
    public int getOverrideDirectoryMode()
    {
        return Integer.parseInt( "0755", 8 );
    }

    @Override
    public void setDefaultDirectoryMode( final int mode )
    {
    }

    @Override
    public int getDefaultDirectoryMode()
    {
        return Integer.parseInt( "0755", 8 );
    }

    @Override
    public boolean getIncludeEmptyDirs()
    {
        return false;
    }

    @Override
    public void setIncludeEmptyDirs( final boolean includeEmptyDirs )
    {
    }

    @Override
    public void setDotFileDirectory( final File dotFileDirectory )
    {
    }

    public @Nonnull
    @Override
    ResourceIterator getResources()
        throws ArchiverException
    {
        throw new RuntimeException( "Not implemented" );
    }

    @Override
    public Map<String, ArchiveEntry> getFiles()
    {
        return new HashMap<>();
    }

    @Override
    public boolean isForced()
    {
        return false;
    }

    @Override
    public void setForced( final boolean forced )
    {
    }

    @Override
    public boolean isSupportingForced()
    {
        return true;
    }

    @Override
    public String getDuplicateBehavior()
    {
        return null;
    }

    @Override
    public void setDuplicateBehavior( final String duplicate )
    {
    }

    public class Addition
    {

        /**
         * {@inheritDoc}
         *
         * @see Object#toString()
         */
        @Override
        public String toString()
        {
            return "Addition (\n    resource= " + resource + "\n    directory= " + directory + "\n    destination= "
                       + destination + "\n    permissions= " + permissions + "\n    includes= "
                       + ( includes == null ? "-none-" : StringUtils.join( includes, ", " ) )
                       + "\n    excludes= "
                       + ( excludes == null ? "-none-" : StringUtils.join( excludes, ", " ) ) + "\n)";

        }

        public final Object resource;

        public final File directory;

        public final String destination;

        public final int permissions;

        public final String[] includes;

        public final String[] excludes;

        public Addition( final Object resource, final String destination, final String[] includes,
                         final String[] excludes, final int permissions )
        {
            this.resource = resource;
            if ( resource instanceof FileSet )
            {
                final FileSet fs = (FileSet) resource;
                directory = fs.getDirectory();
                this.destination = fs.getPrefix();
                this.includes = fs.getIncludes();
                this.excludes = fs.getExcludes();
                this.permissions = permissions;
            }
            else
            {
                if ( resource instanceof File && ( (File) resource ).isDirectory() )
                {
                    directory = (File) resource;
                }
                else
                {
                    directory = null;
                }

                this.destination = destination;
                this.includes = includes;
                this.excludes = excludes;
                this.permissions = permissions;
            }
        }

    }

    @Override
    public boolean isUseJvmChmod()
    {
        return useJvmChmod;
    }

    @Override
    public void setUseJvmChmod( final boolean useJvmChmod )
    {
        this.useJvmChmod = useJvmChmod;
    }

    @Override
    public boolean isIgnorePermissions()
    {
        return ignorePermissions;
    }

    @Override
    public void setIgnorePermissions( final boolean ignorePermissions )
    {
        this.ignorePermissions = ignorePermissions;
    }

    /**
     * @deprecated Use {@link #setLastModifiedTime(FileTime)} instead.
     */
    @Override
    @Deprecated
    public void setLastModifiedDate( final Date lastModifiedDate )
    {
        this.lastModifiedTime = lastModifiedDate != null ? FileTime.fromMillis( lastModifiedDate.getTime() ) : null;
    }

    /**
     * @deprecated Use {@link #getLastModifiedTime()} instead.
     */
    @Override
    @Deprecated
    public Date getLastModifiedDate()
    {
        return lastModifiedTime != null ? new Date( lastModifiedTime.toMillis() ) : null;
    }

    @Override
    public void setLastModifiedTime( final FileTime lastModifiedTime )
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public FileTime getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    @Override
    public void setFilenameComparator( final Comparator<String> filenameComparator )
    {
        this.filenameComparator = filenameComparator;
    }

    public Comparator<String> getFilenameComparator()
    {
        return filenameComparator;
    }

    @Override
    public void setOverrideUid( int uid )
    {
    }

    @Override
    public void setOverrideUserName( String userName )
    {
    }

    @Override
    public int getOverrideUid()
    {
        return 0;
    }

    @Override
    public String getOverrideUserName()
    {
        return null;
    }

    @Override
    public void setOverrideGid( int gid )
    {
    }

    @Override
    public void setOverrideGroupName( String groupName )
    {
    }

    @Override
    public int getOverrideGid()
    {
        return 0;
    }

    @Override
    public String getOverrideGroupName()
    {
        return null;
    }

    /**
     * @deprecated Use {@link #configureReproducibleBuild(FileTime)} instead.
     */
    @Override
    @Deprecated
    public void configureReproducible( Date lastModifiedDate )
    {
    }

    @Override
    public void configureReproducibleBuild( FileTime lastModifiedTime )
    {
    }
}
