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
public interface ArchiverConfigurer {
	
	static ArchiverConfigurer of(Archiver archiver) {
		return new DefaultArchiverConfigurer(archiver);
	}
	
    void addFileSetFromSpec(FileSetSpec fileSetSpec);

    void addArchivedFileSetFromSpec(ArchivedFileSetSpec fileSetSpec);

    void setDestFile(Path destFile);

    /**
     * Sets the permissions used for regular files added to the archive.
     *
     * @param permissions the file permissions
     */
    void setFileMode(FilePermissions permissions);

    /**
     * Sets the default permissions used for regular files when no explicit mode is available.
     *
     * @param permissions the default file permissions
     */
    void setDefaultFileMode(FilePermissions permissions);

    /**
     * Sets the permissions used for directories added to the archive.
     *
     * @param permissions the directory permissions
     */
    void setDirectoryMode(FilePermissions permissions);

    /**
     * Sets the default permissions used for directories when no explicit mode is available.
     *
     * @param permissions the default directory permissions
     */
    void setDefaultDirectoryMode(FilePermissions permissions);

    void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling);

    void setDotFileDirectory(Path dotFileDirectory);

    void setForced(ArchiveCreation archiveCreation);

    void setDuplicateBehavior(DuplicateHandling duplicateHandling);

    void setIgnorePermissions(PermissionHandling permissionHandling);

    void setLastModifiedTime(FileTime lastModifiedTime);

    void setFilenameComparator(Comparator<String> filenameComparator);

    void setOverrideUid(int uid);

    void setOverrideUserName(String userName);

    void setOverrideGid(int gid);

    void setOverrideGroupName(String groupName);

    void setUmask(FilePermissions permissions);

    void configureReproducibleBuild(Consumer<ReproducibleBuildConfigurer> reproducibleBuildSpec);
}
