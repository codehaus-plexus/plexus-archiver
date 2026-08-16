/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.codehaus.plexus.archiver;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

/**
 * Fluent specification of files read from an archive.
 *
 * @since 5.0.0
 */
public final class ArchivedFileSetSpec {
    private final Path archive;
    private String prefix;
    private List<String> includes;
    private List<String> excludes;
    private CaseSensitivity caseSensitivity = CaseSensitivity.SENSITIVE;
    private DefaultExcludes defaultExcludes = DefaultExcludes.USE;
    private EmptyDirectoryHandling emptyDirectoryHandling = EmptyDirectoryHandling.INCLUDE;
    private List<FileSelector> fileSelectors;
    private InputStreamTransformer streamTransformer;
    private List<FileMapper> fileMappers;

    private ArchivedFileSetSpec(Path archive) {
        this.archive = Objects.requireNonNull(archive, "archive");
    }

    static ArchivedFileSetSpec of(Path archive) {
        return new ArchivedFileSetSpec(archive);
    }

    public ArchivedFileSetSpec prefixed(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public ArchivedFileSetSpec including(List<String> includes) {
        this.includes = List.copyOf(includes);
        return this;
    }

    public ArchivedFileSetSpec excluding(List<String> excludes) {
        this.excludes = List.copyOf(excludes);
        return this;
    }

    public ArchivedFileSetSpec caseSensitive(CaseSensitivity caseSensitivity) {
        this.caseSensitivity = Objects.requireNonNull(caseSensitivity, "caseSensitivity");
        return this;
    }

    public ArchivedFileSetSpec usingDefaultExcludes(DefaultExcludes defaultExcludes) {
        this.defaultExcludes = Objects.requireNonNull(defaultExcludes, "defaultExcludes");
        return this;
    }

    public ArchivedFileSetSpec emptyDirectories(EmptyDirectoryHandling emptyDirectoryHandling) {
        this.emptyDirectoryHandling = Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling");
        return this;
    }

    public ArchivedFileSetSpec selectedBy(List<FileSelector> fileSelectors) {
        this.fileSelectors = List.copyOf(fileSelectors);
        return this;
    }

    public ArchivedFileSetSpec transformedBy(InputStreamTransformer streamTransformer) {
        this.streamTransformer = Objects.requireNonNull(streamTransformer, "streamTransformer");
        return this;
    }

    public ArchivedFileSetSpec mappedBy(List<FileMapper> fileMappers) {
        this.fileMappers = List.copyOf(fileMappers);
        return this;
    }

    ArchivedFileSet toArchivedFileSet() {
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
        return defaultExcludes instanceof BuiltInDefaultExcludes builtIn && builtIn.use;
    }

    private boolean includesEmptyDirectories() {
        return ((FixedEmptyDirectoryHandling) emptyDirectoryHandling).included;
    }
}
