package org.codehaus.plexus.archiver;

import java.io.InputStream;

import org.codehaus.plexus.components.io.fileselectors.FileSelector;

/**
 * @deprecated Use {@link FileSelector}
 */
@Deprecated
public interface ArchiveFileFilter {

    boolean include(InputStream dataStream, String entryName);
}
