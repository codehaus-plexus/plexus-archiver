package org.codehaus.plexus.archiver.proxies;

import org.codehaus.plexus.archiver.FileSet;

import java.io.File;

public class DelegatingFileSet extends DelegatingBaseFileSet
    implements FileSet
{

    public DelegatingFileSet( FileSet target )
    {
        super(target);
    }

    public File getDirectory()
    {
        return ((FileSet)target).getDirectory();
    }
}
