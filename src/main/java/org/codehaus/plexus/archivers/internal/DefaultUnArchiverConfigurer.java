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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archivers.spi.ExistingFileHandling;
import org.codehaus.plexus.archivers.spi.PermissionHandling;
import org.codehaus.plexus.archivers.spi.UnArchiverConfigurer;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

public final class DefaultUnArchiverConfigurer implements UnArchiverConfigurer {
    private final UnArchiver unarchiver;

    public DefaultUnArchiverConfigurer(UnArchiver unarchiver) {
        this.unarchiver = Objects.requireNonNull(unarchiver, "unarchiver");
    }

    @Override
    public void setSource(Path source) {
        unarchiver.setSourceFile(Objects.requireNonNull(source, "source").toFile());
    }

    @Override
    public void setDestinationDirectory(Path destinationDirectory) {
        unarchiver.setDestDirectory(Objects.requireNonNull(destinationDirectory, "destinationDirectory")
                .toFile());
    }

    @Override
    public void setDestinationFile(Path destinationFile) {
        unarchiver.setDestFile(
                Objects.requireNonNull(destinationFile, "destinationFile").toFile());
    }

    @Override
    public void setExistingFileHandling(ExistingFileHandling existingFileHandling) {
        unarchiver.setOverwrite(
                Objects.requireNonNull(existingFileHandling, "existingFileHandling") == ExistingFileHandling.OVERWRITE);
    }

    @Override
    public void setFileMappers(List<FileMapper> fileMappers) {
        unarchiver.setFileMappers(List.copyOf(fileMappers).toArray(FileMapper[]::new));
    }

    @Override
    public void setFileSelectors(List<FileSelector> fileSelectors) {
        unarchiver.setFileSelectors(List.copyOf(fileSelectors).toArray(FileSelector[]::new));
    }

    @Override
    public void setPermissionHandling(PermissionHandling permissionHandling) {
        unarchiver.setIgnorePermissions(
                Objects.requireNonNull(permissionHandling, "permissionHandling") == PermissionHandling.IGNORE);
    }
}
