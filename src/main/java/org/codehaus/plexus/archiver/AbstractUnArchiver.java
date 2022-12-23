/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.components.io.attributes.SymlinkUtils;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO there should really be constructors which take the source file.

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
public abstract class AbstractUnArchiver
    implements UnArchiver, FinalizerEnabled
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private File destDirectory;

    private File destFile;

    private File sourceFile;

    private boolean overwrite = true;

    private FileMapper[] fileMappers;

    private List<ArchiveFinalizer> finalizers;

    private FileSelector[] fileSelectors;

    /**
     * since 2.3 is on by default
     *
     * @since 1.1
     */
    private boolean useJvmChmod = true;

    /**
     * @since 1.1
     */
    private boolean ignorePermissions = false;

    public AbstractUnArchiver()
    {
        // no op
    }

    public AbstractUnArchiver( final File sourceFile )
    {
        this.sourceFile = sourceFile;
    }

    @Override
    public File getDestDirectory()
    {
        return destDirectory;
    }

    @Override
    public void setDestDirectory( final File destDirectory )
    {
        this.destDirectory = destDirectory;
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
    public File getSourceFile()
    {
        return sourceFile;
    }

    @Override
    public void setSourceFile( final File sourceFile )
    {
        this.sourceFile = sourceFile;
    }

    @Override
    public boolean isOverwrite()
    {
        return overwrite;
    }

    @Override
    public void setOverwrite( final boolean b )
    {
        overwrite = b;
    }

    @Override
    public FileMapper[] getFileMappers()
    {
        return fileMappers;
    }

    @Override
    public void setFileMappers( final FileMapper[] fileMappers )
    {
        this.fileMappers = fileMappers;
    }

    @Override
    public final void extract()
        throws ArchiverException
    {
        validate();
        execute();
        runArchiveFinalizers();
    }

    @Override
    public final void extract( final String path, final File outputDirectory )
        throws ArchiverException
    {
        validate( path, outputDirectory );
        execute( path, outputDirectory );
        runArchiveFinalizers();
    }

    @Override
    public void addArchiveFinalizer( final ArchiveFinalizer finalizer )
    {
        if ( finalizers == null )
        {
            finalizers = new ArrayList<>();
        }

        finalizers.add( finalizer );
    }

    @Override
    public void setArchiveFinalizers( final List<ArchiveFinalizer> archiveFinalizers )
    {
        finalizers = archiveFinalizers;
    }

    private void runArchiveFinalizers()
        throws ArchiverException
    {
        if ( finalizers != null )
        {
            for ( ArchiveFinalizer finalizer : finalizers )
            {
                finalizer.finalizeArchiveExtraction( this );
            }
        }
    }

    protected void validate( final String path, final File outputDirectory )
    {
    }

    protected void validate()
        throws ArchiverException
    {
        if ( sourceFile == null )
        {
            throw new ArchiverException( "The source file isn't defined." );
        }

        if ( sourceFile.isDirectory() )
        {
            throw new ArchiverException( "The source must not be a directory." );
        }

        if ( !sourceFile.exists() )
        {
            throw new ArchiverException( "The source file " + sourceFile + " doesn't exist." );
        }

        if ( destDirectory == null && destFile == null )
        {
            throw new ArchiverException( "The destination isn't defined." );
        }

        if ( destDirectory != null && destFile != null )
        {
            throw new ArchiverException( "You must choose between a destination directory and a destination file." );
        }

        if ( destDirectory != null && !destDirectory.isDirectory() )
        {
            destFile = destDirectory;
            destDirectory = null;
        }

        if ( destFile != null && destFile.isDirectory() )
        {
            destDirectory = destFile;
            destFile = null;
        }
    }

    @Override
    public void setFileSelectors( final FileSelector[] fileSelectors )
    {
        this.fileSelectors = fileSelectors;
    }

    @Override
    public FileSelector[] getFileSelectors()
    {
        return fileSelectors;
    }

    protected boolean isSelected( final String fileName, final PlexusIoResource fileInfo )
        throws ArchiverException
    {
        if ( fileSelectors != null )
        {
            for ( FileSelector fileSelector : fileSelectors )
            {
                try
                {

                    if ( !fileSelector.isSelected( fileInfo ) )
                    {
                        return false;
                    }
                }
                catch ( final IOException e )
                {
                    throw new ArchiverException(
                        "Failed to check, whether " + fileInfo.getName() + " is selected: " + e.getMessage(), e );
                }
            }
        }
        return true;
    }

    protected abstract void execute()
        throws ArchiverException;

    protected abstract void execute( String path, File outputDirectory )
        throws ArchiverException;

    /**
     * @since 1.1
     */
    @Override
    public boolean isUseJvmChmod()
    {
        return useJvmChmod;
    }

    /**
     * <b>jvm chmod won't set group level permissions !</b>
     *
     * @since 1.1
     */
    @Override
    public void setUseJvmChmod( final boolean useJvmChmod )
    {
        this.useJvmChmod = useJvmChmod;
    }

    /**
     * @since 1.1
     */
    @Override
    public boolean isIgnorePermissions()
    {
        return ignorePermissions;
    }

    /**
     * @since 1.1
     */
    @Override
    public void setIgnorePermissions( final boolean ignorePermissions )
    {
        this.ignorePermissions = ignorePermissions;
    }

    protected void extractFile( final File srcF, final File dir, final InputStream compressedInputStream,
                                String entryName, final Date entryDate, final boolean isDirectory,
                                final Integer mode, String symlinkDestination, final FileMapper[] fileMappers )
        throws IOException, ArchiverException
    {
        if ( fileMappers != null )
        {
            for ( final FileMapper fileMapper : fileMappers )
            {
                entryName = fileMapper.getMappedFileName( entryName );
            }
        }

        // Hmm. Symlinks re-evaluate back to the original file here. Unsure if this is a good thing...
        final File targetFileName = FileUtils.resolveFile( dir, entryName );

        // Make sure that the resolved path of the extracted file doesn't escape the destination directory
        String canonicalDirPath = dir.getCanonicalPath();
        String canonicalDestPath = targetFileName.getCanonicalPath();

        if ( !canonicalDestPath.startsWith( canonicalDirPath ) )
        {
            throw new ArchiverException( "Entry is outside of the target directory (" + entryName + ")" );
        }

        try
        {
            if ( !shouldExtractEntry( dir, targetFileName, entryName, entryDate ) )
            {
                return;
            }

            // create intermediary directories - sometimes zip don't add them
            final File dirF = targetFileName.getParentFile();
            if ( dirF != null )
            {
                dirF.mkdirs();
            }

            if ( !StringUtils.isEmpty( symlinkDestination ) )
            {
                SymlinkUtils.createSymbolicLink( targetFileName, new File( symlinkDestination ) );
            }
            else if ( isDirectory )
            {
                targetFileName.mkdirs();
            }
            else
            {
                try ( OutputStream out = Files.newOutputStream( targetFileName.toPath() ) )
                {
                    IOUtil.copy( compressedInputStream, out );
                }
            }

            targetFileName.setLastModified( entryDate.getTime() );

            if ( !isIgnorePermissions() && mode != null && !isDirectory )
            {
                ArchiveEntryUtils.chmod( targetFileName, mode );
            }
        }
        catch ( final FileNotFoundException ex )
        {
            getLogger().warn( "Unable to expand to file " + targetFileName.getPath() );
        }
    }

    /**
     * Counter for casing message emitted, visible for testing.
     */
    final AtomicInteger casingMessageEmitted = new AtomicInteger( 0 );

    // Visible for testing
    protected boolean shouldExtractEntry( File targetDirectory, File targetFileName, String entryName, Date entryDate ) throws IOException
    {
        //     entryname  | entrydate | filename   | filedate | behavior
        // (1) readme.txt | 1970      | -          | -        | always extract if the file does not exist
        // (2) readme.txt | 1970      | readme.txt | 2020     | do not overwrite unless isOverwrite() is true
        // (3) readme.txt | 2020      | readme.txt | 1970     | always override when the file is older than the archive entry
        // (4) README.txt | 1970      | readme.txt | 2020     | case-insensitive filesystem: warn + do not overwrite unless isOverwrite()
        //                                                      case-sensitive filesystem: extract without warning
        // (5) README.txt | 2020      | readme.txt | 1970     | case-insensitive filesystem: warn + overwrite because entry is newer
        //                                                      case-sensitive filesystem: extract without warning

        // The canonical file name follows the name of the archive entry, but takes into account the case-
        // sensitivity of the filesystem. So on a case-sensitive file system, file.exists() returns false for
        // scenario (4) and (5).
        // No matter the case sensitivity of the file system, file.exists() returns false when there is no file with the same name (1).
        if ( !targetFileName.exists() )
        {
            return true;
        }

        boolean entryIsDirectory = entryName.endsWith( "/" ); // directory entries always end with '/', regardless of the OS.
        String canonicalDestPath = targetFileName.getCanonicalPath();
        String suffix = (entryIsDirectory ? "/" : "");
        String relativeCanonicalDestPath = canonicalDestPath.replace(
                targetDirectory.getCanonicalPath() + File.separatorChar,
                "" )
                + suffix;
        boolean fileOnDiskIsOlderThanEntry = targetFileName.lastModified() < entryDate.getTime();
        boolean differentCasing = !normalizedFileSeparator( entryName ).equals( normalizedFileSeparator( relativeCanonicalDestPath ) );

        // Warn for case (4) and (5) if the file system is case-insensitive
        if ( differentCasing )
        {
            String casingMessage = String.format( Locale.ENGLISH, "Archive entry '%s' and existing file '%s' names differ only by case."
                + " This may lead to an unexpected outcome on case-insensitive filesystems.", entryName, canonicalDestPath );
            getLogger().warn( casingMessage );
            casingMessageEmitted.incrementAndGet();
        }

        // Override the existing file if isOverwrite() is true or if the file on disk is older than the one in the archive
        return isOverwrite() || fileOnDiskIsOlderThanEntry;
    }
    
    private String normalizedFileSeparator(String pathOrEntry) {
    	return pathOrEntry.replace("/", File.separator);
    }
}
