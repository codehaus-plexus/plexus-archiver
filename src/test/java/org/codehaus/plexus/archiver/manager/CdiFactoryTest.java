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

package org.codehaus.plexus.archiver.manager;

import java.nio.file.Path;

import org.codehaus.plexus.archiver.zip.PlexusArchiverZipFileResourceCollection;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoArchiveResourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class CdiFactoryTest {

    @Test
    void createsConfiguredArchivers(@TempDir Path directory) {
        CdiArchiverFactory factory = new CdiArchiverFactory(ZipArchiver::new);

        var first = factory.create(configurer -> configurer.setDestFile(directory.resolve("first.zip")));
        var second = factory.create(configurer -> configurer.setDestFile(directory.resolve("second.zip")));

        assertThat(first).isNotSameAs(second);
        assertThat(first.getDestFile()).isEqualTo(directory.resolve("first.zip").toFile());
        assertThat(second.getDestFile())
                .isEqualTo(directory.resolve("second.zip").toFile());
    }

    @Test
    void createsConfiguredUnarchivers(@TempDir Path directory) {
        CdiUnArchiverFactory factory = new CdiUnArchiverFactory(ZipUnArchiver::new);

        var unarchiver = factory.create(configurer -> configurer.setSource(directory.resolve("source.zip")));

        assertThat(unarchiver.getSourceFile())
                .isEqualTo(directory.resolve("source.zip").toFile());
    }

    @Test
    void createsConfiguredResourceCollections(@TempDir Path directory) {
        CdiPlexusIoResourceCollectionFactory factory =
                new CdiPlexusIoResourceCollectionFactory(PlexusArchiverZipFileResourceCollection::new);

        var collection = (AbstractPlexusIoArchiveResourceCollection)
                factory.create(configurer -> configurer.setSource(directory.resolve("source.zip")));

        assertThat(collection.getFile())
                .isEqualTo(directory.resolve("source.zip").toFile());
    }
}
