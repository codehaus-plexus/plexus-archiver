package org.codehaus.plexus.archiver.snappy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.inject.Named;

import org.codehaus.plexus.archiver.util.Streams;
import org.codehaus.plexus.components.io.attributes.FileAttributes;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;

/**
 * Implementation of {@link org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection} for
 * snappy compressed files.
 */
@Named( "snappy" )
public class PlexusIoSnappyResourceCollection
    extends PlexusIoCompressedFileResourceCollection
{

    @Nonnull
    @Override
    protected @WillNotClose
    InputStream getInputStream( File file )
        throws IOException
    {
        return SnappyUnArchiver.getSnappyInputStream( Streams.fileInputStream( file ) );
    }

    @Override protected PlexusIoResourceAttributes getAttributes( File file ) throws IOException
    {
        return new FileAttributes( file, new HashMap<Integer, String>(), new HashMap<Integer, String>() );
    }

    @Override
    protected String getDefaultExtension()
    {
        return ".snappy";
    }

}
