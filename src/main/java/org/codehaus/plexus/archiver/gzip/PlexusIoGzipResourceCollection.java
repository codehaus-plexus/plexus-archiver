package org.codehaus.plexus.archiver.gzip;

import static org.codehaus.plexus.archiver.util.Streams.bufferedInputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;
import javax.inject.Named;

import org.codehaus.plexus.components.io.attributes.FileAttributes;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;

/**
 * Abstract base class for compressed files, aka singleton
 * resource collections.
 */
@Named( "gzip" )
public class PlexusIoGzipResourceCollection
    extends PlexusIoCompressedFileResourceCollection
{

    @Override
    protected String getDefaultExtension()
    {
        return ".gz";
    }

    @Nonnull
    @Override
    protected InputStream getInputStream( File file )
        throws IOException
    {
        return bufferedInputStream( new GZIPInputStream( fileInputStream( file ) ) );
    }

    @Override
    protected PlexusIoResourceAttributes getAttributes( File file )
        throws IOException
    {
        return new FileAttributes( file, new HashMap<Integer, String>(), new HashMap<Integer, String>() );
    }

}
