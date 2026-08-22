package org.codehaus.plexus.archiver;

import java.io.File;
import java.nio.file.Path;

/**
 * A file set, which consists of the files and directories in
 * a common base directory.
 *
 * @since 1.0-alpha-9
 */
public interface FileSet extends BaseFileSet {

    /**
     * Creates a fluent file set specification.
     *
     * @param directory the file set base directory
     * @return a file set for the directory
     * @since 5.0.0
     */
    static FileSetSpec of(Path directory) {
        return FileSetSpec.of(directory);
    }

    /**
     * Returns the file sets base directory.
     */
    File getDirectory();
}
