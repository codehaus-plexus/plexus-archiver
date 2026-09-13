package org.codehaus.plexus.archiver.tar;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.attributes.SimpleResourceAttributes;
import org.codehaus.plexus.components.io.functions.HardLinkIdentitySupplier;
import org.codehaus.plexus.components.io.functions.ResourceAttributeSupplier;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

public class TarResource extends AbstractPlexusIoResource
        implements ResourceAttributeSupplier, HardLinkIdentitySupplier {

    private final TarFile tarFile;

    private final TarArchiveEntry entry;

    private PlexusIoResourceAttributes attributes;

    public TarResource(TarFile tarFile, TarArchiveEntry entry) {
        super(
                entry.getName(),
                getLastModifiedTime(entry),
                entry.isDirectory() ? PlexusIoResource.UNKNOWN_RESOURCE_SIZE : entry.getSize(),
                !entry.isDirectory(),
                entry.isDirectory(),
                true);

        this.tarFile = tarFile;
        this.entry = entry;
    }

    private static long getLastModifiedTime(TarArchiveEntry entry) {
        long l = entry.getModTime().getTime();
        return l == -1 ? PlexusIoResource.UNKNOWN_MODIFICATION_DATE : l;
    }

    @Override
    public synchronized PlexusIoResourceAttributes getAttributes() {
        if (attributes == null) {
            TarArchiveEntry entry = contentEntry();
            attributes = new SimpleResourceAttributes(
                    entry.getUserId(), entry.getUserName(), entry.getGroupId(), entry.getGroupName(), entry.getMode());
        }

        return attributes;
    }

    public synchronized void setAttributes(PlexusIoResourceAttributes attributes) {
        this.attributes = attributes;
    }

    /** Returns the resolved data size while retaining zero in the actual TAR link header. */
    @Override
    public long getSize() {
        return entry.isLink() ? contentEntry().getRealSize() : super.getSize();
    }

    /** Uses data-bearing metadata so aliases of one inode have compatible archive attributes. */
    @Override
    public long getLastModified() {
        return entry.isLink() ? getLastModifiedTime(contentEntry()) : super.getLastModified();
    }

    /** Resolves link metadata without changing the archive enumeration cursor. */
    private TarArchiveEntry contentEntry() {
        try {
            return entry.isLink() ? tarFile.resolve(entry) : entry;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Supplies an identity scoped to this reader and the exact data-bearing occurrence.
     * Subclasses must explicitly supply an identity that guarantees their actual contents.
     * @return the payload identity, or null for subclasses, directories, and symbolic links
     * @throws IOException if a hard-link relationship is invalid
     */
    @Override
    public Object getHardLinkIdentity() throws IOException {
        // An overriding content supplier may expose different bytes from this archive occurrence.
        if (getClass() != TarResource.class) {
            return null;
        }
        TarArchiveEntry target = tarFile.resolve(entry);
        return TarEntryIndex.isRegular(target)
                ? Arrays.asList(tarFile, tarFile.index().position(target))
                : null;
    }

    @Override
    public URL getURL() throws IOException {
        return null;
    }

    @Nonnull
    @Override
    public InputStream getContents() throws IOException {
        return tarFile.getInputStream(entry);
    }
}
