package org.codehaus.plexus.archiver.tar;

import javax.inject.Named;

import java.io.File;

@Named( "tar.bz2" )
public class PlexusIoTarBZip2FileResourceCollection
    extends PlexusIoTarFileResourceCollection
{

    @Override
    protected TarFile newTarFile( File file )
    {
        return new BZip2TarFile( file );
    }

}
