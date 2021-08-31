package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;

public class AnonymousResource extends AbstractPlexusIoResource
{

    private final File file;

    public AnonymousResource( File file )
    {
        this( file, getName( file ) );
    }

    public AnonymousResource( File file, String name )
    {
        super( name, file.lastModified(), file.length(), file.isFile(), file.isDirectory(), file.exists() );
        this.file = file;
    }

    @Nonnull
    @Override
    public InputStream getContents()
        throws IOException
    {
        throw new UnsupportedOperationException( "not supp" );
    }

    @Override
    public URL getURL()
        throws IOException
    {
        return file.toURI().toURL();
    }

    private static String getName( File file )
    {
        return file.getPath().replace( '\\', '/' );
    }

}
