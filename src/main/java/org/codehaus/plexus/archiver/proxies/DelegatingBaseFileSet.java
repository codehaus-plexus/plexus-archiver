package org.codehaus.plexus.archiver.proxies;

import org.codehaus.plexus.archiver.BaseFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

import javax.annotation.CheckForNull;

public class DelegatingBaseFileSet
{
    protected final BaseFileSet target;

    public DelegatingBaseFileSet( BaseFileSet target )
    {
        this.target = target;
    }

    @CheckForNull
    public String getPrefix()
    {
        return target.getPrefix();
    }

    public boolean isUsingDefaultExcludes()
    {
        return target.isUsingDefaultExcludes();
    }

    @CheckForNull
    public String[] getExcludes()
    {
        return target.getExcludes();
    }

    @CheckForNull
    public String[] getIncludes()
    {
        return target.getIncludes();
    }

    public InputStreamTransformer getStreamTransformer()
    {
        return target.getStreamTransformer();
    }

    public boolean isCaseSensitive()
    {
        return target.isCaseSensitive();
    }

    @CheckForNull
    public FileSelector[] getFileSelectors()
    {
        return target.getFileSelectors();
    }

    public boolean isIncludingEmptyDirectories()
    {
        return target.isIncludingEmptyDirectories();
    }
}
