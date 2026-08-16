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

package org.codehaus.plexus.archivers.spi;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.CaseSensitivities;
import org.codehaus.plexus.archiver.CaseSensitivity;
import org.codehaus.plexus.archiver.DefaultExcludes;
import org.codehaus.plexus.archiver.EmptyDirectoryHandling;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoArchiveResourceCollection;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResourceCollection;
import org.codehaus.plexus.components.io.resources.EncodingSupported;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

/**
 * Configures a Plexus IO resource collection before it is exposed to callers.
 *
 * @since 5.0.0
 */
public final class PlexusIoResourceCollectionConfigurer {
    private final PlexusIoResourceCollection collection;

    PlexusIoResourceCollectionConfigurer(PlexusIoResourceCollection collection) {
        this.collection = Objects.requireNonNull(collection, "collection");
    }

    public void setSource(Path source) {
        Objects.requireNonNull(source, "source");
        if (collection instanceof AbstractPlexusIoArchiveResourceCollection archiveCollection) {
            archiveCollection.setFile(source.toFile());
        } else if (collection instanceof PlexusIoCompressedFileResourceCollection compressedCollection) {
            compressedCollection.setFile(source.toFile());
        } else if (collection instanceof PlexusIoFileResourceCollection fileCollection) {
            fileCollection.setBaseDir(source.toFile());
        } else {
            throw unsupported("source paths");
        }
    }

    public void setPrefix(String prefix) {
        configurableCollection().setPrefix(Objects.requireNonNull(prefix, "prefix"));
    }

    public void setIncludes(List<String> includes) {
        configurableCollection().setIncludes(List.copyOf(includes).toArray(String[]::new));
    }

    public void setExcludes(List<String> excludes) {
        configurableCollection().setExcludes(List.copyOf(excludes).toArray(String[]::new));
    }

    public void setFileSelectors(List<FileSelector> fileSelectors) {
        configurableCollection().setFileSelectors(List.copyOf(fileSelectors).toArray(FileSelector[]::new));
    }

    public void setFileMappers(List<FileMapper> fileMappers) {
        configurableCollection().setFileMappers(List.copyOf(fileMappers).toArray(FileMapper[]::new));
    }

    public void setStreamTransformer(InputStreamTransformer streamTransformer) {
        configurableCollection().setStreamTransformer(Objects.requireNonNull(streamTransformer, "streamTransformer"));
    }

    public void setCaseSensitivity(CaseSensitivity caseSensitivity) {
        configurableCollection().setCaseSensitive(CaseSensitivities.resolve(caseSensitivity));
    }

    public void setDefaultExcludes(DefaultExcludes defaultExcludes) {
        Objects.requireNonNull(defaultExcludes, "defaultExcludes");
        configurableCollection().setUsingDefaultExcludes(defaultExcludes == DefaultExcludes.USE);
    }

    public void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling) {
        Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling");
        configurableCollection().setIncludingEmptyDirectories(emptyDirectoryHandling == EmptyDirectoryHandling.INCLUDE);
    }

    public void setEncoding(Charset charset) {
        if (!(collection instanceof EncodingSupported encodingSupported)) {
            throw unsupported("filename encoding");
        }
        encodingSupported.setEncoding(Objects.requireNonNull(charset, "charset"));
    }

    public void setSymbolicLinkHandling(SymbolicLinkHandling symbolicLinkHandling) {
        Objects.requireNonNull(symbolicLinkHandling, "symbolicLinkHandling");
        if (!(collection instanceof PlexusIoFileResourceCollection fileCollection)) {
            throw unsupported("symbolic-link traversal");
        }
        fileCollection.setFollowingSymLinks(((FixedSymbolicLinkHandling) symbolicLinkHandling).follow);
    }

    private UnsupportedOperationException unsupported(String capability) {
        return new UnsupportedOperationException(collection.getClass().getName() + " does not support " + capability);
    }

    private AbstractPlexusIoResourceCollection configurableCollection() {
        if (collection instanceof AbstractPlexusIoResourceCollection configurableCollection) {
            return configurableCollection;
        }
        throw unsupported("common resource collection configuration");
    }
}
