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
package org.codehaus.plexus.archiver.gzip;

import static org.codehaus.plexus.archiver.util.Streams.bufferedOutputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.Compressor;

public class GZipCompressor
    extends Compressor
{

    private OutputStream zOut;

    /**
     * perform the GZip compression operation.
     */
    @Override
    public void compress()
        throws ArchiverException
    {
        try
        {
            zOut = bufferedOutputStream( new GZIPOutputStream( fileOutputStream( getDestFile() ) ) );
            compress( getSource(), zOut );
        }
        catch ( IOException ioe )
        {
            String msg = "Problem creating gzip " + ioe.getMessage();
            throw new ArchiverException( msg, ioe );
        }
    }

    @Override
    public void close()
    {
        try
        {
            if ( this.zOut != null )
            {
                this.zOut.close();
                zOut = null;
            }
        }
        catch ( final IOException e )
        {
            throw new ArchiverException( "Failure closing target.", e );
        }
    }

}
