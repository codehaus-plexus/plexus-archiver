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
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.ArchivedFileSetSpec;
import org.codehaus.plexus.archiver.EmptyDirectoryHandling;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.FileSetSpec;

/**
 * Configures content on an archiver without exposing its file set implementations.
 *
 * @since 5.0.0
 */
public interface ArchiverConfigurer {
    void addFileSet(FileSet fileSet);

    void addFileSet(FileSetSpec fileSetSpec);

    void addArchivedFileSet(ArchivedFileSet fileSet);

    void addArchivedFileSet(ArchivedFileSetSpec fileSetSpec);

    void setDestFile(Path destFile);

    void setFileMode(UnixPermissions permissions);

    void setDefaultFileMode(UnixPermissions permissions);

    void setDirectoryMode(UnixPermissions permissions);

    void setDefaultDirectoryMode(UnixPermissions permissions);

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

    void setUmask(UnixPermissions permissions);

    void configureReproducibleBuild(FileTime lastModifiedTime);
}
