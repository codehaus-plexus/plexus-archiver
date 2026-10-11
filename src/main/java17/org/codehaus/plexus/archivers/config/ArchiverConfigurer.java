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

package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;

/**
 * Configures content on an archiver without exposing its file set implementations.
 *
 * @since 5.0.0
 */
public sealed interface ArchiverConfigurer permits DefaultArchiverConfigurer {
	
	static ArchiverConfigurer of(Archiver archiver) {
		return new DefaultArchiverConfigurer(archiver);
	}
	
	ArchiverConfigurer resource(
            ResourceScan scan,
            Consumer<ResourceEmitConfigurer> emit);
	
    void setDestFile(Path destFile);

    void setFileMode(FileMode fileMode);

    void setDirectoryMode(DirectoryMode directoryMode);

    void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling);

    void setDotFileDirectory(Path dotFileDirectory);

    void setForced(ArchiveCreation archiveCreation);

    void setDuplicateBehavior(DuplicateHandling duplicateHandling);

    void setIgnorePermissions(PermissionHandling permissionHandling);

    void setUmask(FilePermissions permissions);

    void configureReproducibleBuild(Consumer<ReproducibleBuildConfigurer> reproducibleBuildSpec);
}
