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
package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import javax.annotation.Nonnull;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
public abstract class AbstractZipUnArchiver
    extends AbstractUnArchiver
{

    private static final String NATIVE_ENCODING = "native-encoding";

    private String encoding = "UTF8";

    private long maxOutputSize = Long.MAX_VALUE;

    public AbstractZipUnArchiver()
    {
    }

    public AbstractZipUnArchiver( final File sourceFile )
    {
        super( sourceFile );
    }

    /**
     * Sets the encoding to assume for file names and comments.
     * <p>
     * Set to <code>native-encoding</code> if you want your platform's native encoding, defaults to UTF8.
     * </p>
     */
    public void setEncoding( String encoding )
    {
        if ( NATIVE_ENCODING.equals( encoding ) )
        {
            encoding = null;
        }
        this.encoding = encoding;
    }

    /**
     * Set maximum allowed size of the produced output.
     *
     * It may be used as a protection against <a href="https://en.wikipedia.org/wiki/Zip_bomb">zip bombs</a>.
     *
     * @param maxOutputSize max size of the produced output, in bytes. Must be greater than 0
     * @throws IllegalArgumentException if specified output size is less or equal to 0
     */
    public void setMaxOutputSize( long maxOutputSize ) {
        if ( maxOutputSize <= 0 ) {
            throw new IllegalArgumentException( "Invalid max output size specified: " + maxOutputSize );
        }
        this.maxOutputSize = maxOutputSize;
    }

    private static class ZipEntryFileInfo
        implements PlexusIoResource
    {

        private final org.apache.commons.compress.archivers.zip.ZipFile zipFile;

        private final ZipArchiveEntry zipEntry;

        ZipEntryFileInfo( final org.apache.commons.compress.archivers.zip.ZipFile zipFile,
                          final ZipArchiveEntry zipEntry )
        {
            this.zipFile = zipFile;
            this.zipEntry = zipEntry;
        }

        public String getName()
        {
            return zipEntry.getName();
        }

        @Override
        public boolean isDirectory()
        {
            return zipEntry.isDirectory();
        }

        @Override
        public boolean isFile()
        {
            return !zipEntry.isDirectory() && !zipEntry.isUnixSymlink();
        }

        @Override
        public boolean isSymbolicLink()
        {
            return zipEntry.isUnixSymlink();
        }

        @Nonnull
        @Override
        public InputStream getContents()
            throws IOException
        {
            return zipFile.getInputStream( zipEntry );
        }

        @Override
        public long getLastModified()
        {
            final long l = zipEntry.getTime();
            return l == 0 ? PlexusIoResource.UNKNOWN_MODIFICATION_DATE : l;
        }

        @Override
        public long getSize()
        {
            final long l = zipEntry.getSize();
            return l == -1 ? PlexusIoResource.UNKNOWN_RESOURCE_SIZE : l;
        }

        @Override
        public URL getURL()
            throws IOException
        {
            return null;
        }

        @Override
        public boolean isExisting()
        {
            return true;
        }

    }

    @Override
    protected void execute()
        throws ArchiverException
    {
        execute( "", getDestDirectory() );
    }

    private String resolveSymlink( ZipFile zf, ZipArchiveEntry ze )
        throws IOException
    {
        if ( ze.isUnixSymlink() )
        {
            return zf.getUnixSymlink( ze );
        }
        else
        {
            return null;
        }
    }

    @Override
    protected void execute( final String path, final File outputDirectory )
        throws ArchiverException
    {
        getLogger().debug( "Expanding: " + getSourceFile() + " into " + outputDirectory );
        try ( ZipFile zipFile = new ZipFile( getSourceFile(), encoding, true ) )
        {
            long remainingSpace = maxOutputSize;
            final Enumeration<ZipArchiveEntry> e = zipFile.getEntriesInPhysicalOrder();

            while ( e.hasMoreElements() )
            {
                final ZipArchiveEntry ze = e.nextElement();
                final ZipEntryFileInfo fileInfo = new ZipEntryFileInfo( zipFile, ze );
                if ( !isSelected( ze.getName(), fileInfo ) )
                {
                    continue;
                }

                if ( ze.getName().startsWith( path ) )
                {
                    try ( InputStream in = zipFile.getInputStream( ze ) )
                    {
                        BoundedInputStream bis = new BoundedInputStream( in, remainingSpace + 1 );
                        CountingInputStream cis = new CountingInputStream( bis );
                        extractFile( getSourceFile(), outputDirectory, cis,
                                     ze.getName(), new Date( ze.getTime() ), ze.isDirectory(),
                                     ze.getUnixMode() != 0 ? ze.getUnixMode() : null,
                                     resolveSymlink( zipFile, ze ), getFileMappers() );

                        remainingSpace -= cis.getByteCount();
                        if ( remainingSpace < 0 )
                        {
                            throw new ArchiverException( "Maximum output size limit reached" );
                        }
                    }
                }
            }
            getLogger().debug( "expand complete" );
        }
        catch ( final IOException ioe )
        {
            throw new ArchiverException( "Error while expanding " + getSourceFile().getAbsolutePath(), ioe );
        }
    }

}
