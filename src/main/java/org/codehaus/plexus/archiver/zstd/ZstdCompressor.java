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

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.Compressor;

import java.io.BufferedOutputStream;
import java.io.IOException;

import static org.codehaus.plexus.archiver.util.Streams.bufferedOutputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileOutputStream;

/**
 * Zstd compression
 */
public class ZstdCompressor extends Compressor
{

    private Integer level;

    private ZstdCompressorOutputStream zstdOut;

    public ZstdCompressor()
    {
    }

    public void setLevel( Integer level )
    {
        this.level = level;
    }

    @Override
    public void compress() throws ArchiverException
    {
        try
        {
            BufferedOutputStream outStream = bufferedOutputStream( fileOutputStream( getDestFile() ) );
            if (level == null)
            {
                zstdOut = new ZstdCompressorOutputStream( outStream );
            }
            else
            {
                zstdOut = new ZstdCompressorOutputStream( outStream, level );
            }
            compress( getSource(), zstdOut);
        }
        catch ( IOException ioe )
        {
            throw new ArchiverException( "Problem creating zstd " + ioe.getMessage(), ioe );
        }
    }

    @Override
    public void close()
    {
        try
        {
            if ( this.zstdOut != null )
            {
                this.zstdOut.close();
                zstdOut = null;
            }
        }
        catch ( final IOException e )
        {
            throw new ArchiverException( "Failure closing target.", e );
        }
    }

}
