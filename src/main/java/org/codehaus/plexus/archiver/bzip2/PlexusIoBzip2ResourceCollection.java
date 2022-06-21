package org.codehaus.plexus.archiver.bzip2;

import static org.codehaus.plexus.archiver.util.Streams.fileInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.inject.Named;

import org.codehaus.plexus.components.io.attributes.FileAttributes;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

/**
 * Implementation of {@link PlexusIoResourceCollection} for
 * bzip2 compressed files.
 */
@Named( "bzip2" )
public class PlexusIoBzip2ResourceCollection
    extends PlexusIoCompressedFileResourceCollection
{

    @Nonnull
    @Override
    protected @WillNotClose
    InputStream getInputStream( File file )
        throws IOException
    {
        return BZip2UnArchiver.getBZip2InputStream( fileInputStream( file ) );
    }

    @Override protected PlexusIoResourceAttributes getAttributes( File file ) throws IOException
    {
        return new FileAttributes( file, new HashMap<Integer, String>(), new HashMap<Integer, String>() );
    }

    @Override
    protected String getDefaultExtension()
    {
        return ".bz2";
    }

}
