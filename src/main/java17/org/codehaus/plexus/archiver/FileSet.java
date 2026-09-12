package org.codehaus.plexus.archiver;


import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archivers.config.CaseSensitivities;
import org.codehaus.plexus.archivers.config.CaseSensitivity;
import org.codehaus.plexus.archivers.config.DefaultExcludes;
import org.codehaus.plexus.archivers.config.EmptyDirectoryHandling;
import org.codehaus.plexus.archivers.config.SymbolicLinkHandling;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

/**
 * A file set, which consists of the files and directories in
 * a common base directory.
 *
 * @since 1.0-alpha-9
 */
public interface FileSet extends BaseFileSet {

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

    /**
     * 
     * @param directory
     * @return
     * @since 5.0.0
     */
    public static Builder fromDirectory(Path directory) {
        return new Builder(directory);
    }

    /**
     * @since 5.0.0
     */
    static final class Builder {
        private final Path directory;
        private String prefix;
        private Collection<String> includes;
        private Collection<String> excludes;
        private CaseSensitivity caseSensitivity = CaseSensitivity.SENSITIVE;
        private DefaultExcludes defaultExcludes = DefaultExcludes.USE;
        private EmptyDirectoryHandling emptyDirectoryHandling = EmptyDirectoryHandling.INCLUDE;
        private SymbolicLinkHandling symbolicLinkHandling = SymbolicLinkHandling.PRESERVE;
        private Collection<FileSelector> fileSelectors;
        private InputStreamTransformer streamTransformer;
        private Collection<FileMapper> fileMappers;

        private Builder(Path directory) {
            this.directory = Objects.requireNonNull(directory, "directory");
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder includes(Collection<String> includes) {
            this.includes = List.copyOf(includes);
            return this;
        }

        public Builder excludes(Collection<String> excludes) {
            this.excludes = List.copyOf(excludes);
            return this;
        }

        public Builder caseSensitive(CaseSensitivity caseSensitivity) {
            this.caseSensitivity = Objects.requireNonNull(caseSensitivity, "caseSensitivity");
            return this;
        }

        public Builder defaultExcludes(DefaultExcludes defaultExcludes) {
            this.defaultExcludes = Objects.requireNonNull(defaultExcludes, "defaultExcludes");
            return this;
        }

        public Builder emptyDirectories(EmptyDirectoryHandling emptyDirectoryHandling) {
            this.emptyDirectoryHandling = Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling");
            return this;
        }

        public Builder symbolicLinks(SymbolicLinkHandling symbolicLinkHandling) {
            this.symbolicLinkHandling = Objects.requireNonNull(symbolicLinkHandling, "symbolicLinkHandling");
            return this;
        }

        public Builder fileSelectors(Collection<FileSelector> fileSelectors) {
            this.fileSelectors = List.copyOf(fileSelectors);
            return this;
        }

        public Builder streamTransformer(InputStreamTransformer streamTransformer) {
            this.streamTransformer = Objects.requireNonNull(streamTransformer, "streamTransformer");
            return this;
        }

        public Builder fileMappers(Collection<FileMapper> fileMappers) {
            this.fileMappers = List.copyOf(fileMappers);
            return this;
        }

        public FileSet build() {
            DefaultFileSet fileSet = new DefaultFileSet(directory.toFile());
            fileSet.setPrefix(prefix);
            fileSet.setIncludes(includes == null ? null : includes.toArray(String[]::new));
            fileSet.setExcludes(excludes == null ? null : excludes.toArray(String[]::new));
            fileSet.setCaseSensitive(isCaseSensitive());
            fileSet.setUsingDefaultExcludes(usesBuiltInDefaultExcludes());
            fileSet.setIncludingEmptyDirectories(includesEmptyDirectories());
            fileSet.setFollowingSymLinks(followsSymbolicLinks());
            fileSet.setFileSelectors(fileSelectors == null ? null : fileSelectors.toArray(FileSelector[]::new));
            if (streamTransformer != null) {
                fileSet.setStreamTransformer(streamTransformer);
            }
            fileSet.setFileMappers(fileMappers == null ? null : fileMappers.toArray(FileMapper[]::new));
            return fileSet;
        }

        private boolean isCaseSensitive() {
            return CaseSensitivities.resolve(caseSensitivity);
        }

        private boolean usesBuiltInDefaultExcludes() {
            return defaultExcludes == DefaultExcludes.USE;
        }

        private boolean followsSymbolicLinks() {
            return symbolicLinkHandling == SymbolicLinkHandling.FOLLOW;
        }

        private boolean includesEmptyDirectories() {
            return emptyDirectoryHandling == EmptyDirectoryHandling.INCLUDE;
        }
    }
}
