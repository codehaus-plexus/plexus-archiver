package org.codehaus.plexus.archiver.tar;

import javax.inject.Named;

import java.io.File;

@Named( "tar.snappy" )
public class PlexusIoTarSnappyFileResourceCollection
    extends PlexusIoTarFileResourceCollection
{

    @Override
    protected TarFile newTarFile( File file )
    {
        return new SnappyTarFile( file );
    }

}
