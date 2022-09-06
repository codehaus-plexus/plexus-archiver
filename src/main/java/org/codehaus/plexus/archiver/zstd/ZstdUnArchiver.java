/*
 * Copyright 2022 The Apache Software Foundation
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
package org.codehaus.plexus.archiver.zstd;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.codehaus.plexus.archiver.util.Streams.bufferedInputStream;
import static org.codehaus.plexus.archiver.util.Streams.bufferedOutputStream;
import static org.codehaus.plexus.archiver.util.Streams.copyFully;
import static org.codehaus.plexus.archiver.util.Streams.fileInputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileOutputStream;

/**
 * Unarchiver for zstd-compressed files.
 */
@Named( "zst" )
public class ZstdUnArchiver extends AbstractUnArchiver
{

    private static final String OPERATION_ZSTD = "zstd";

    public ZstdUnArchiver()
    {
    }

    public ZstdUnArchiver( File source )
    {
        super( source );
    }

    @Override
    protected void execute() throws ArchiverException
    {
        if ( getSourceFile().lastModified() > getDestFile().lastModified() )
        {
            getLogger().info( "Expanding " + getSourceFile().getAbsolutePath() + " to "
                                  + getDestFile().getAbsolutePath() );

            copyFully( getZstdInputStream( bufferedInputStream( fileInputStream( getSourceFile(), OPERATION_ZSTD) ) ),
                       bufferedOutputStream( fileOutputStream( getDestFile(), OPERATION_ZSTD) ), OPERATION_ZSTD);

        }
    }

    public static @Nonnull
    ZstdCompressorInputStream getZstdInputStream( InputStream in )
        throws ArchiverException
    {
        try
        {
            return new ZstdCompressorInputStream( in );
        }
        catch ( IOException ioe )
        {
            throw new ArchiverException( "Trouble creating Zstd compressor, invalid file ?", ioe );
        }
    }

    @Override
    protected void execute( String path, File outputDirectory ) throws ArchiverException
    {
        throw new UnsupportedOperationException( "Targeted execution not supported in zstd format" );
    }

}
