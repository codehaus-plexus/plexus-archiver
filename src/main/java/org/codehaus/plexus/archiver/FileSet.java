package org.codehaus.plexus.archiver;

import java.io.File;
import java.nio.file.Path;

import org.codehaus.plexus.archiver.util.DefaultFileSet;

/**
 * A file set, which consists of the files and directories in
 * a common base directory.
 *
 * @since 1.0-alpha-9
 */
public interface FileSet extends BaseFileSet {

    static FileSet of(Path path) {
        return new DefaultFileSet(path.toFile());
    }

    /**
     * Returns the file sets base directory.
     */
    File getDirectory();
}
