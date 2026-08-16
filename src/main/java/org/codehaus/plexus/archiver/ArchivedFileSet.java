package org.codehaus.plexus.archiver;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.file.Path;

/**
 * A file set, which consists of the files and directories in
 * an archive.
 *
 * @since 1.0-alpha-9
 */
public interface ArchivedFileSet extends BaseFileSet {

    /**
     * Creates a fluent archived file set specification.
     *
     * @param archive the source archive
     * @return a file set for the archive
     * @since 5.0.0
     */
    static ArchivedFileSetSpec of(Path archive) {
        return ArchivedFileSetSpec.of(archive);
    }

    /**
     * Returns the archive file.
     */
    @CheckForNull
    File getArchive();
}
