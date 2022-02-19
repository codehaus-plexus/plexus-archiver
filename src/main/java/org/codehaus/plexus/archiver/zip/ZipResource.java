package org.codehaus.plexus.archiver.zip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.Nonnull;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.codehaus.plexus.archiver.UnixStat;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.attributes.SimpleResourceAttributes;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.functions.ResourceAttributeSupplier;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;
import org.codehaus.plexus.components.io.resources.ClosingInputStream;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

public class ZipResource extends AbstractPlexusIoResource
    implements ResourceAttributeSupplier
{

    private final org.apache.commons.compress.archivers.zip.ZipFile zipFile;

    private final ZipArchiveEntry entry;

    private final InputStreamTransformer streamTransformer;

    private PlexusIoResourceAttributes attributes;

    public ZipResource( ZipFile zipFile, ZipArchiveEntry entry, InputStreamTransformer streamTransformer )
    {
        super( entry.getName(), getLastModified( entry ),
               entry.isDirectory() ? PlexusIoResource.UNKNOWN_RESOURCE_SIZE : entry.getSize(), !entry.isDirectory(),
               entry.isDirectory(), true );

        this.zipFile = zipFile;
        this.entry = entry;
        this.streamTransformer = streamTransformer;
    }

    private static long getLastModified( ZipArchiveEntry entry )
    {
        long time = entry.getTime();
        return time == -1 ? PlexusIoResource.UNKNOWN_MODIFICATION_DATE : time;
    }

    @Override
    public synchronized PlexusIoResourceAttributes getAttributes()
    {
        int mode = PlexusIoResourceAttributes.UNKNOWN_OCTAL_MODE;
        if ( entry.getPlatform() == ZipArchiveEntry.PLATFORM_UNIX )
        {
            mode = entry.getUnixMode();
            if ( ( mode & UnixStat.FILE_FLAG ) == UnixStat.FILE_FLAG )
            {
                mode = mode & ~UnixStat.FILE_FLAG;
            }
            else
            {
                mode = mode & ~UnixStat.DIR_FLAG;
            }
        }

        if ( attributes == null )
        {
            attributes = new SimpleResourceAttributes( null, null, null, null, mode );
        }

        return attributes;
    }

    public synchronized void setAttributes( PlexusIoResourceAttributes attributes )
    {
        this.attributes = attributes;
    }

    @Override
    public URL getURL()
        throws IOException
    {
        return null;
    }

    @Nonnull
    @Override
    public InputStream getContents()
        throws IOException
    {
        final InputStream inputStream = zipFile.getInputStream( entry );
        return new ClosingInputStream( streamTransformer.transform( this, inputStream ), inputStream );
    }

}
