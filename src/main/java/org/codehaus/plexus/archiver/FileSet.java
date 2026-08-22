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

    /**
     * Returns whether symbolic links below the base directory are followed.
     * <p>
     * When {@code false} (the default), a symbolic link is added to the archive as a link and the
     * contents of a linked directory are not scanned. When {@code true}, links are resolved: a
     * linked directory is added as a directory and its target's contents are included.
     * <p>
     * Following links means the scan can descend into a directory that links back to one of its own
     * ancestors, so only enable it for trees known not to contain such cycles.
     *
     * @since 4.14.0
     */
    default boolean isFollowingSymLinks() {
        return false;
    }
}
