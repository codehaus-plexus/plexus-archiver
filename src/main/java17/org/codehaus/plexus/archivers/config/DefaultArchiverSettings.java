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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

final class DefaultArchiverSettings implements ArchiverSettings {

    private final Archiver archiver;

    DefaultArchiverSettings(Archiver archiver) {
        this.archiver = Objects.requireNonNull(archiver, "archiver");
    }

    @Override
    public void addResource(ResourceScan scan, Consumer<ResourceEmitSettings> emit) {
        Objects.requireNonNull(scan, "scan");
        Objects.requireNonNull(emit, "emit");

        if (scan instanceof DefaultDirectoryResourceScan directoryScan) {
            DefaultFileSet fileSet = directoryScan.toFileSet();
            emit.accept(new FileSetResourceEmitSettings(fileSet));
            archiver.addFileSet(fileSet);
            return;
        }

        if (scan instanceof DefaultArchiveResourceScan archiveScan) {
            DefaultArchivedFileSet fileSet = archiveScan.toArchivedFileSet();
            emit.accept(new FileSetResourceEmitSettings(fileSet));
            archiver.addArchivedFileSet(fileSet);
            return;
        }

        throw new IncompatibleClassChangeError(
                "Unsupported ResourceScan implementation: " + scan.getClass().getName());
    }

    @Override
    public void setDestFile(Path destFile) {
        archiver.setDestFile(Objects.requireNonNull(destFile, "destFile").toFile());
    }

    @Override
    public void setEmptyDirectoryHandling(EmptyDirectoryHandling handling) {
        archiver.setIncludeEmptyDirs(Objects.requireNonNull(handling, "handling") == EmptyDirectoryHandling.INCLUDE);
    }

    @Override
    public void setArchiveCreation(ArchiveCreation archiveCreation) {
        archiver.setForced(Objects.requireNonNull(archiveCreation, "archiveCreation") == ArchiveCreation.ALWAYS);
    }

    @Override
    public void setDuplicateHandling(DuplicateHandling duplicateHandling) {
        Objects.requireNonNull(duplicateHandling, "duplicateHandling");
        String behavior = duplicateHandling == DuplicateHandling.ADD ? Archiver.DUPLICATES_ADD
				: duplicateHandling == DuplicateHandling.PRESERVE ? Archiver.DUPLICATES_PRESERVE
						: duplicateHandling == DuplicateHandling.SKIP ? Archiver.DUPLICATES_SKIP
								: Archiver.DUPLICATES_FAIL;
        archiver.setDuplicateBehavior(behavior);
    }

    @Override
    public void setPermissionPolicy(PermissionPolicy policy) {
        Objects.requireNonNull(policy, "policy");

        if (policy instanceof IgnorePermissionPolicy) {
            applyIgnorePolicy();
            return;
        }
        if (policy instanceof UmaskPermissionPolicy umaskPolicy) {
            applyUmaskPolicy(umaskPolicy);
            return;
        }
        if (policy instanceof EntryModesPermissionPolicy entryModesPolicy) {
            applyEntryModesPolicy(entryModesPolicy);
            return;
        }

        throw new IncompatibleClassChangeError(
                "Unsupported PermissionPolicy implementation: " + policy.getClass().getName());
    }

    private void applyIgnorePolicy() {
        archiver.setIgnorePermissions(true);
        clearUmask();
        resetModesToInherit();
    }

    private void applyUmaskPolicy(UmaskPermissionPolicy policy) {
        archiver.setIgnorePermissions(false);
        archiver.setUmask(toMode(policy.mask()));
        resetModesToInherit();
    }

    private void applyEntryModesPolicy(EntryModesPermissionPolicy policy) {
        archiver.setIgnorePermissions(false);
        clearUmask();
        applyFileMode(policy.files());
        applyDirectoryMode(policy.directories());
    }

    private void applyFileMode(FileMode fileMode) {
        if (fileMode instanceof OverrideFileMode mode) {
            archiver.setFileMode(toMode(mode.permissions()));
            archiver.setDefaultFileMode(Archiver.DEFAULT_FILE_MODE);
            return;
        }
        if (fileMode instanceof FallbackFileMode mode) {
            archiver.setFileMode(-1);
            archiver.setDefaultFileMode(toMode(mode.permissions()));
            return;
        }
        if (fileMode instanceof InheritedFileMode) {
            archiver.setFileMode(-1);
            archiver.setDefaultFileMode(Archiver.DEFAULT_FILE_MODE);
            return;
        }
        throw new IncompatibleClassChangeError(
                "Unsupported FileMode implementation: " + fileMode.getClass().getName());
    }

    private void applyDirectoryMode(DirectoryMode directoryMode) {
        if (directoryMode instanceof OverrideDirectoryMode mode) {
            archiver.setDirectoryMode(toMode(mode.permissions()));
            archiver.setDefaultDirectoryMode(Archiver.DEFAULT_DIR_MODE);
            return;
        }
        if (directoryMode instanceof FallbackDirectoryMode mode) {
            archiver.setDirectoryMode(-1);
            archiver.setDefaultDirectoryMode(toMode(mode.permissions()));
            return;
        }
        if (directoryMode instanceof InheritedDirectoryMode) {
            archiver.setDirectoryMode(-1);
            archiver.setDefaultDirectoryMode(Archiver.DEFAULT_DIR_MODE);
            return;
        }
        throw new IncompatibleClassChangeError(
                "Unsupported DirectoryMode implementation: " + directoryMode.getClass().getName());
    }

    private void resetModesToInherit() {
        archiver.setFileMode(-1);
        archiver.setDefaultFileMode(Archiver.DEFAULT_FILE_MODE);
        archiver.setDirectoryMode(-1);
        archiver.setDefaultDirectoryMode(Archiver.DEFAULT_DIR_MODE);
    }

    private void clearUmask() {
        archiver.setUmask(-1);
    }

    private static int toMode(FilePermissions filePermissions) {
        if (filePermissions instanceof PosixPermissions permissions) {
            int mode = 0;
            for (PosixFilePermission permission : permissions.permissions()) {
                mode |= switch (permission) {
                    case OWNER_READ -> 0400;
                    case OWNER_WRITE -> 0200;
                    case OWNER_EXECUTE -> 0100;
                    case GROUP_READ -> 0040;
                    case GROUP_WRITE -> 0020;
                    case GROUP_EXECUTE -> 0010;
                    case OTHERS_READ -> 0004;
                    case OTHERS_WRITE -> 0002;
                    case OTHERS_EXECUTE -> 0001;
                };
            }
            return mode;
        }

        if (filePermissions instanceof ModePermissions permissions) {
            return permissions.mode();
        }

        throw new IncompatibleClassChangeError(
                "Unsupported FilePermissions implementation: " + filePermissions.getClass().getName());
    }
}