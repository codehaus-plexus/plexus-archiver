package org.codehaus.plexus.archiver.util;

import javax.annotation.Nonnull;

import java.io.File;

import org.codehaus.plexus.archiver.FileSet;

/**
 * Default implementation of {@link FileSet}.
 *
 * @since 1.0-alpha-9
 */
public class DefaultFileSet extends AbstractFileSet<DefaultFileSet> implements FileSet {

    private File directory;

    private boolean followingSymLinks;

    public DefaultFileSet(File directory) {
        this.directory = directory;
    }

    public DefaultFileSet() {}

    /**
     * Sets the file sets base directory.
     */
    public void setDirectory(@Nonnull File directory) {
        this.directory = directory;
    }

    @Nonnull
    public File getDirectory() {
        return directory;
    }

    /**
     * Sets whether symbolic links below the base directory are followed. Defaults to false.
     *
     * @since 4.14.0
     */
    public void setFollowingSymLinks(boolean followingSymLinks) {
        this.followingSymLinks = followingSymLinks;
    }

    @Override
    public boolean isFollowingSymLinks() {
        return followingSymLinks;
    }

    /**
     * @since 4.14.0
     */
    public DefaultFileSet followingSymLinks(boolean followingSymLinks) {
        setFollowingSymLinks(followingSymLinks);
        return this;
    }

    public static DefaultFileSet fileSet(File directory) {
        final DefaultFileSet defaultFileSet = new DefaultFileSet(directory);
        return defaultFileSet;
    }
}
