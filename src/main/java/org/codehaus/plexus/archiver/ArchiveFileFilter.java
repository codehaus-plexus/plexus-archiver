package org.codehaus.plexus.archiver;

import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.InputStream;

/**
 * @deprecated Use {@link FileSelector}
 */
@Deprecated
public interface ArchiveFileFilter
{

    boolean include( InputStream dataStream, String entryName );

}
