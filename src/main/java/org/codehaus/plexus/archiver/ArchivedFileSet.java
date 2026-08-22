package org.codehaus.plexus.archiver;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.file.Path;

import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;

/**
 * A file set, which consists of the files and directories in
 * an archive.
 *
 * @since 1.0-alpha-9
 */
public interface ArchivedFileSet extends BaseFileSet {

    public static ArchivedFileSet of(Path path) {
        return new DefaultArchivedFileSet(path.toFile());
    }

    /**
     * Returns the archive file.
     */
    @CheckForNull
    File getArchive();
}
