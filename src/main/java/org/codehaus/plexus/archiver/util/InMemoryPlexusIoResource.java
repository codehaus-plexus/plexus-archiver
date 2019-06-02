package org.codehaus.plexus.archiver.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;

/**
 * In-memory virtual plexus IO resource
 *
 */
public class InMemoryPlexusIoResource extends AbstractPlexusIoResource
{

    public InMemoryPlexusIoResource( String name, long lastModified, long size, boolean isFile, boolean isDirectory,
                                   boolean isExisting )
    {
        super( name, lastModified, size, isFile, isDirectory, isExisting );
    }

    @Override
    public InputStream getContents()
        throws IOException
    {
        return null;
    }

    @Override
    public URL getURL()
        throws IOException
    {
        return null;
    }
    
}
