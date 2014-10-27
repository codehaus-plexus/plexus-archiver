package org.codehaus.plexus.archiver.proxies;

import org.codehaus.plexus.archiver.ArchivedFileSet;

import java.io.File;

public class DelegatingArchivedFileSet
    extends DelegatingBaseFileSet implements ArchivedFileSet
{
    public DelegatingArchivedFileSet( ArchivedFileSet target )
    {
        super( target );
    }

    public File getArchive()
    {
        return ((ArchivedFileSet)target).getArchive();
    }
}
