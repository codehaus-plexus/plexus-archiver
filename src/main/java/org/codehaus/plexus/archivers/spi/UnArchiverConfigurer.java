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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

/**
 * Configures an unarchiver before it is exposed to callers.
 *
 * @since 5.0.0
 */
public final class UnArchiverConfigurer {
    private final UnArchiver unarchiver;

    UnArchiverConfigurer(UnArchiver unarchiver) {
        this.unarchiver = unarchiver;
    }

    public void setSource(Path source) {
        unarchiver.setSourceFile(Objects.requireNonNull(source, "source").toFile());
    }

    public void setDestinationDirectory(Path destinationDirectory) {
        unarchiver.setDestDirectory(Objects.requireNonNull(destinationDirectory, "destinationDirectory")
                .toFile());
    }

    public void setDestinationFile(Path destinationFile) {
        unarchiver.setDestFile(
                Objects.requireNonNull(destinationFile, "destinationFile").toFile());
    }

    public void setExistingFileHandling(ExistingFileHandling existingFileHandling) {
        Objects.requireNonNull(existingFileHandling, "existingFileHandling");
        unarchiver.setOverwrite(((FixedExistingFileHandling) existingFileHandling).overwrite);
    }

    public void setFileMappers(List<FileMapper> fileMappers) {
        unarchiver.setFileMappers(List.copyOf(fileMappers).toArray(FileMapper[]::new));
    }

    public void setFileSelectors(List<FileSelector> fileSelectors) {
        unarchiver.setFileSelectors(List.copyOf(fileSelectors).toArray(FileSelector[]::new));
    }

    public void setPermissionHandling(PermissionHandling permissionHandling) {
        Objects.requireNonNull(permissionHandling, "permissionHandling");
        unarchiver.setIgnorePermissions(((FixedPermissionHandling) permissionHandling).ignored);
    }
}
