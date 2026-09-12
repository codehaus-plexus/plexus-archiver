package org.codehaus.plexus.archiver;


import javax.annotation.CheckForNull;

import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.archivers.config.CaseSensitivities;
import org.codehaus.plexus.archivers.config.CaseSensitivity;
import org.codehaus.plexus.archivers.config.DefaultExcludes;
import org.codehaus.plexus.archivers.config.EmptyDirectoryHandling;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A file set, which consists of the files and directories in
 * an archive.
 *
 * @since 1.0-alpha-9
 */
public interface ArchivedFileSet extends BaseFileSet {

    /**
     * Returns the archive file.
     */
    @CheckForNull
    File getArchive();
    

    /**
     * 
     * @param archive
     * @return
     * @since 5.0.0
     */
    public static Builder of(Path archive) {
        return new Builder(archive);
    }

    /**
     * @since 5.0.0
     */
    static final class Builder {
        private final Path archive;
        private String prefix;
        private Collection<String> includes;
        private Collection<String> excludes;
        private CaseSensitivity caseSensitivity = CaseSensitivity.SENSITIVE;
        private DefaultExcludes defaultExcludes = DefaultExcludes.USE;
        private EmptyDirectoryHandling emptyDirectoryHandling = EmptyDirectoryHandling.INCLUDE;
        private Collection<FileSelector> fileSelectors;
        private InputStreamTransformer streamTransformer;
        private Collection<FileMapper> fileMappers;

        private Builder(Path archive) {
            this.archive = Objects.requireNonNull(archive, "archive");
        }

        public Builder prefixed(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder including(Collection<String> includes) {
            this.includes = List.copyOf(includes);
            return this;
        }

        public Builder excluding(Collection<String> excludes) {
            this.excludes = List.copyOf(excludes);
            return this;
        }

        public Builder caseSensitive(CaseSensitivity caseSensitivity) {
            this.caseSensitivity = Objects.requireNonNull(caseSensitivity, "caseSensitivity");
            return this;
        }

        public Builder usingDefaultExcludes(DefaultExcludes defaultExcludes) {
            this.defaultExcludes = Objects.requireNonNull(defaultExcludes, "defaultExcludes");
            return this;
        }

        public Builder emptyDirectories(EmptyDirectoryHandling emptyDirectoryHandling) {
            this.emptyDirectoryHandling = Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling");
            return this;
        }

        public Builder selectedBy(Collection<FileSelector> fileSelectors) {
            this.fileSelectors = List.copyOf(fileSelectors);
            return this;
        }

        public Builder transformedBy(InputStreamTransformer streamTransformer) {
            this.streamTransformer = Objects.requireNonNull(streamTransformer, "streamTransformer");
            return this;
        }

        public Builder mappedBy(Collection<FileMapper> fileMappers) {
            this.fileMappers = List.copyOf(fileMappers);
            return this;
        }

        public ArchivedFileSet build() {
            DefaultArchivedFileSet fileSet = new DefaultArchivedFileSet(archive.toFile());
            fileSet.setPrefix(prefix);
            fileSet.setIncludes(includes == null ? null : includes.toArray(String[]::new));
            fileSet.setExcludes(excludes == null ? null : excludes.toArray(String[]::new));
            fileSet.setCaseSensitive(isCaseSensitive());
            fileSet.setUsingDefaultExcludes(usesBuiltInDefaultExcludes());
            fileSet.setIncludingEmptyDirectories(includesEmptyDirectories());
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

        private boolean includesEmptyDirectories() {
            return emptyDirectoryHandling == EmptyDirectoryHandling.INCLUDE;
        }
    }
}
