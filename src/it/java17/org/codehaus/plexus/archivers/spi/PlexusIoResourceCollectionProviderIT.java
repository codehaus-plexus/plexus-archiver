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

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.archiver.PlexusIoResourceCollectionConfigurer;
import org.codehaus.plexus.archiver.SymbolicLinkHandling;
import org.codehaus.plexus.archiver.gzip.PlexusIoGzipResourceCollectionProvider;
import org.codehaus.plexus.archiver.CaseSensitivity;
import org.codehaus.plexus.archiver.DefaultExcludes;
import org.codehaus.plexus.archiver.EmptyDirectoryHandling;
import org.codehaus.plexus.archiver.resources.PlexusIoFileResourceCollectionProvider;
import org.codehaus.plexus.archiver.zip.PlexusArchiverZipFileResourceCollectionProvider;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoArchiveResourceCollection;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoCompressedFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class PlexusIoResourceCollectionProviderIT {

    @Test
    void exposesOnlyConfiguredCreation() {
        assertThat(PlexusIoResourceCollectionProvider.class.getPermittedSubclasses())
                .containsExactly(AbstractPlexusIoResourceCollectionProvider.class);
        assertThat(Arrays.stream(PlexusIoResourceCollectionProvider.class.getMethods())
                        .filter(method -> method.getName().equals("newPlexusIoResourceCollection"))
                        .map(method -> method.getParameterCount()))
                .containsExactly(1);
        assertThat(Arrays.stream(AbstractPlexusIoResourceCollectionProvider.class.getMethods())
                        .map(method -> method.getName()))
                .doesNotContain("create");
        assertThat(Arrays.stream(PlexusIoResourceCollectionConfigurer.class.getMethods())
                        .filter(method -> Modifier.isStatic(method.getModifiers())))
                .isEmpty();
    }

    @Test
    void configuresArchiveResourceCollection(@TempDir Path directory) {
        Path source = directory.resolve("source.zip");

        AbstractPlexusIoResourceCollection collection = (AbstractPlexusIoResourceCollection)
                new PlexusArchiverZipFileResourceCollectionProvider().newPlexusIoResourceCollection(configurer -> {
                    configurer.setSource(source);
                    configurer.setPrefix("content/");
                    configurer.setIncludes(List.of("**/*.txt"));
                    configurer.setExcludes(List.of("**/ignored.txt"));
                    configurer.setCaseSensitivity(CaseSensitivity.INSENSITIVE);
                    configurer.setDefaultExcludes(DefaultExcludes.IGNORE);
                    configurer.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
                    configurer.setEncoding(StandardCharsets.UTF_8);
                });

        assertThat(((AbstractPlexusIoArchiveResourceCollection) collection).getFile())
                .isEqualTo(source.toFile());
        assertThat(collection.getPrefix()).isEqualTo("content/");
        assertThat(collection.getIncludes()).containsExactly("**/*.txt");
        assertThat(collection.getExcludes()).containsExactly("**/ignored.txt");
        assertThat(collection.isCaseSensitive()).isFalse();
        assertThat(collection.isUsingDefaultExcludes()).isFalse();
        assertThat(collection.isIncludingEmptyDirectories()).isFalse();
    }

    @Test
    void configuresFilesystemResourceCollection(@TempDir Path directory) {
        PlexusIoFileResourceCollection collection = (PlexusIoFileResourceCollection)
                new PlexusIoFileResourceCollectionProvider().newPlexusIoResourceCollection(configurer -> {
                    configurer.setSource(directory);
                    configurer.setSymbolicLinkHandling(SymbolicLinkHandling.DO_NOT_FOLLOW);
                });

        assertThat(collection.getBaseDir()).isEqualTo(directory.toFile());
        assertThat(collection.isFollowingSymLinks()).isFalse();
    }

    @Test
    void configuresCompressedResourceCollectionSource(@TempDir Path directory) {
        Path source = directory.resolve("source.gz");

        PlexusIoCompressedFileResourceCollection collection =
                (PlexusIoCompressedFileResourceCollection) new PlexusIoGzipResourceCollectionProvider()
                        .newPlexusIoResourceCollection(configurer -> configurer.setSource(source));

        assertThat(collection.getFile()).isEqualTo(source.toFile());
    }
}
