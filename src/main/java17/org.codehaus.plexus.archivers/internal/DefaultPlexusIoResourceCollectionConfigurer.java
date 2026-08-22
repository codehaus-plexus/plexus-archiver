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

package org.codehaus.plexus.archivers.internal;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.CaseSensitivities;
import org.codehaus.plexus.archiver.CaseSensitivity;
import org.codehaus.plexus.archiver.DefaultExcludes;
import org.codehaus.plexus.archiver.EmptyDirectoryHandling;
import org.codehaus.plexus.archiver.PlexusIoResourceCollectionConfigurer;
import org.codehaus.plexus.archiver.SymbolicLinkHandling;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoArchiveResourceCollection;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResourceCollection;
import org.codehaus.plexus.components.io.resources.EncodingSupported;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

public final class DefaultPlexusIoResourceCollectionConfigurer implements PlexusIoResourceCollectionConfigurer {
    private final PlexusIoResourceCollection collection;

    public DefaultPlexusIoResourceCollectionConfigurer(PlexusIoResourceCollection collection) {
        this.collection = Objects.requireNonNull(collection, "collection");
    }

    @Override
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

    @Override
    public void setPrefix(String prefix) {
        configurableCollection().setPrefix(Objects.requireNonNull(prefix, "prefix"));
    }

    @Override
    public void setIncludes(List<String> includes) {
        configurableCollection().setIncludes(List.copyOf(includes).toArray(String[]::new));
    }

    @Override
    public void setExcludes(List<String> excludes) {
        configurableCollection().setExcludes(List.copyOf(excludes).toArray(String[]::new));
    }

    @Override
    public void setFileSelectors(List<FileSelector> fileSelectors) {
        configurableCollection().setFileSelectors(List.copyOf(fileSelectors).toArray(FileSelector[]::new));
    }

    @Override
    public void setFileMappers(List<FileMapper> fileMappers) {
        configurableCollection().setFileMappers(List.copyOf(fileMappers).toArray(FileMapper[]::new));
    }

    @Override
    public void setStreamTransformer(InputStreamTransformer streamTransformer) {
        configurableCollection().setStreamTransformer(Objects.requireNonNull(streamTransformer, "streamTransformer"));
    }

    @Override
    public void setCaseSensitivity(CaseSensitivity caseSensitivity) {
        configurableCollection().setCaseSensitive(CaseSensitivities.resolve(caseSensitivity));
    }

    @Override
    public void setDefaultExcludes(DefaultExcludes defaultExcludes) {
        configurableCollection()
                .setUsingDefaultExcludes(
                        Objects.requireNonNull(defaultExcludes, "defaultExcludes") == DefaultExcludes.USE);
    }

    @Override
    public void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling) {
        configurableCollection()
                .setIncludingEmptyDirectories(Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling")
                        == EmptyDirectoryHandling.INCLUDE);
    }

    @Override
    public void setEncoding(Charset charset) {
        if (!(collection instanceof EncodingSupported encodingSupported)) {
            throw unsupported("filename encoding");
        }
        encodingSupported.setEncoding(Objects.requireNonNull(charset, "charset"));
    }

    @Override
    public void setSymbolicLinkHandling(SymbolicLinkHandling symbolicLinkHandling) {
        Objects.requireNonNull(symbolicLinkHandling, "symbolicLinkHandling");
        if (!(collection instanceof PlexusIoFileResourceCollection fileCollection)) {
            throw unsupported("symbolic-link traversal");
        }
        fileCollection.setFollowingSymLinks(symbolicLinkHandling == SymbolicLinkHandling.FOLLOW);
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
