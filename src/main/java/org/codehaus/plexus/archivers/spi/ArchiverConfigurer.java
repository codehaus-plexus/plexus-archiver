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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Objects;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.ArchivedFileSetSpec;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.EmptyDirectoryHandling;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.FileSetSpec;

/**
 * Configures content on an archiver without exposing its file set implementations.
 *
 * @since 5.0.0
 */
public final class ArchiverConfigurer {

    private final Archiver archiver;

    ArchiverConfigurer(Archiver archiver) {
        this.archiver = archiver;
    }

    public void addFileSet(FileSet fileSet) {
        archiver.addFileSet(Objects.requireNonNull(fileSet, "fileSet"));
    }

    public void addFileSet(FileSetSpec fileSetSpec) {
        archiver.addFileSet(Objects.requireNonNull(fileSetSpec, "fileSetSpec"));
    }

    public void addArchivedFileSet(ArchivedFileSet fileSet) {
        archiver.addArchivedFileSet(Objects.requireNonNull(fileSet, "fileSet"));
    }

    public void addArchivedFileSet(ArchivedFileSetSpec fileSetSpec) {
        archiver.addArchivedFileSet(Objects.requireNonNull(fileSetSpec, "fileSetSpec"));
    }

    public void setDestFile(Path destFile) {
        archiver.setDestFile(Objects.requireNonNull(destFile, "destFile").toFile());
    }

    public void setFileMode(UnixPermissions permissions) {
        archiver.setFileMode(toMode(permissions));
    }

    public void setDefaultFileMode(UnixPermissions permissions) {
        archiver.setDefaultFileMode(toMode(permissions));
    }

    public void setDirectoryMode(UnixPermissions permissions) {
        archiver.setDirectoryMode(toMode(permissions));
    }

    public void setDefaultDirectoryMode(UnixPermissions permissions) {
        archiver.setDefaultDirectoryMode(toMode(permissions));
    }

    public void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling) {
        Objects.requireNonNull(emptyDirectoryHandling, "emptyDirectoryHandling");
        archiver.setIncludeEmptyDirs(emptyDirectoryHandling == EmptyDirectoryHandling.INCLUDE);
    }

    public void setDotFileDirectory(Path dotFileDirectory) {
        archiver.setDotFileDirectory(
                Objects.requireNonNull(dotFileDirectory, "dotFileDirectory").toFile());
    }

    public void setForced(ArchiveCreation archiveCreation) {
        Objects.requireNonNull(archiveCreation, "archiveCreation");
        archiver.setForced(((FixedArchiveCreation) archiveCreation).forced);
    }

    public void setDuplicateBehavior(DuplicateHandling duplicateHandling) {
        Objects.requireNonNull(duplicateHandling, "duplicateHandling");
        archiver.setDuplicateBehavior(((FixedDuplicateHandling) duplicateHandling).value);
    }

    public void setIgnorePermissions(PermissionHandling permissionHandling) {
        Objects.requireNonNull(permissionHandling, "permissionHandling");
        archiver.setIgnorePermissions(((FixedPermissionHandling) permissionHandling).ignored);
    }

    public void setLastModifiedTime(FileTime lastModifiedTime) {
        archiver.setLastModifiedTime(Objects.requireNonNull(lastModifiedTime, "lastModifiedTime"));
    }

    public void setFilenameComparator(Comparator<String> filenameComparator) {
        archiver.setFilenameComparator(Objects.requireNonNull(filenameComparator, "filenameComparator"));
    }

    public void setOverrideUid(int uid) {
        if (uid < 0) {
            throw new IllegalArgumentException("uid must not be negative");
        }
        archiver.setOverrideUid(uid);
    }

    public void setOverrideUserName(String userName) {
        archiver.setOverrideUserName(Objects.requireNonNull(userName, "userName"));
    }

    public void setOverrideGid(int gid) {
        if (gid < 0) {
            throw new IllegalArgumentException("gid must not be negative");
        }
        archiver.setOverrideGid(gid);
    }

    public void setOverrideGroupName(String groupName) {
        archiver.setOverrideGroupName(Objects.requireNonNull(groupName, "groupName"));
    }

    public void setUmask(UnixPermissions permissions) {
        archiver.setUmask(toMode(permissions));
    }

    public void configureReproducibleBuild(FileTime lastModifiedTime) {
        archiver.configureReproducibleBuild(Objects.requireNonNull(lastModifiedTime, "lastModifiedTime"));
    }

    private static int toMode(UnixPermissions unixPermissions) {
        Objects.requireNonNull(unixPermissions, "unixPermissions");
        PosixPermissions posixPermissions = (PosixPermissions) unixPermissions;
        int mode = 0;
        for (PosixFilePermission permission : posixPermissions.permissions) {
            mode |= switch (permission) {
                case OWNER_READ -> 0_400;
                case OWNER_WRITE -> 0_200;
                case OWNER_EXECUTE -> 0_100;
                case GROUP_READ -> 0_040;
                case GROUP_WRITE -> 0_020;
                case GROUP_EXECUTE -> 0_010;
                case OTHERS_READ -> 0_004;
                case OTHERS_WRITE -> 0_002;
                case OTHERS_EXECUTE -> 0_001;
            };
        }
        return mode;
    }
}
