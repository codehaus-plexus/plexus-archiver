package org.codehaus.plexus.archiver.tar;

import javax.inject.Named;

import java.io.File;

/**
 * Alias for {@link PlexusIoTarGZipFileResourceCollection}
 */
@Named( "tgz" )
public class PlexusIoTGZFileResourceCollection
    extends PlexusIoTarGZipFileResourceCollection
{

    @Override
    protected TarFile newTarFile( File file )
    {
        return new GZipTarFile( file );
    }

}
