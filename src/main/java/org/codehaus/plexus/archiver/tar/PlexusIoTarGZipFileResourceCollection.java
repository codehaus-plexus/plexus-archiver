package org.codehaus.plexus.archiver.tar;

import javax.inject.Named;

import java.io.File;

@Named( "tar.gz" )
public class PlexusIoTarGZipFileResourceCollection
    extends PlexusIoTarFileResourceCollection
{

    @Override
    protected TarFile newTarFile( File file )
    {
        return new GZipTarFile( file );
    }

}
