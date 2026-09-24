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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;

/**
 * Applies the configuration to the applied archiver
 *
 * @since 5.0.0
 */
final class DefaultArchiverConfigurer implements ArchiverConfigurer {
    private final Archiver archiver;

    DefaultArchiverConfigurer(Archiver archiver) {
        this.archiver = Objects.requireNonNull(archiver, "archiver");
    }

    @Override
    public void addFileSet(FileSet fileSet) {
        archiver.addFileSet(Objects.requireNonNull(fileSet, "fileSet"));
    }

    @Override
    public void addArchivedFileSet(ArchivedFileSet fileSet) {
        archiver.addArchivedFileSet(Objects.requireNonNull(fileSet, "fileSet"));
    }

    @Override
    public void setDestFile(Path destFile) {
        archiver.setDestFile(Objects.requireNonNull(destFile, "destFile").toFile());
    }

    @Override
    public void setFileMode(FilePermissions permissions) {
        archiver.setFileMode(toMode(permissions));
    }

    @Override
    public void setDefaultFileMode(FilePermissions permissions) {
        archiver.setDefaultFileMode(toMode(permissions));
    }

    @Override
    public void setDirectoryMode(FilePermissions permissions) {
        archiver.setDirectoryMode(toMode(permissions));
    }

    @Override
    public void setDefaultDirectoryMode(FilePermissions permissions) {
        archiver.setDefaultDirectoryMode(toMode(permissions));
    }

    @Override
    public void setEmptyDirectoryHandling(EmptyDirectoryHandling handling) {
        archiver.setIncludeEmptyDirs(Objects.requireNonNull(handling, "handling") == EmptyDirectoryHandling.INCLUDE);
    }

    @Override
    public void setDotFileDirectory(Path dotFileDirectory) {
        archiver.setDotFileDirectory(
                Objects.requireNonNull(dotFileDirectory, "dotFileDirectory").toFile());
    }

    @Override
    public void setForced(ArchiveCreation archiveCreation) {
        archiver.setForced(Objects.requireNonNull(archiveCreation, "archiveCreation") == ArchiveCreation.ALWAYS);
    }

    @Override
    public void setDuplicateBehavior(DuplicateHandling duplicateHandling) {
        Objects.requireNonNull(duplicateHandling, "duplicateHandling");
        String behavior = duplicateHandling == DuplicateHandling.ADD
                ? Archiver.DUPLICATES_ADD
                : duplicateHandling == DuplicateHandling.PRESERVE
                        ? Archiver.DUPLICATES_PRESERVE
                        : duplicateHandling == DuplicateHandling.SKIP
                                ? Archiver.DUPLICATES_SKIP
                                : Archiver.DUPLICATES_FAIL;
        archiver.setDuplicateBehavior(behavior);
    }

    @Override
    public void setIgnorePermissions(PermissionHandling permissionHandling) {
        archiver.setIgnorePermissions(
                Objects.requireNonNull(permissionHandling, "permissionHandling") == PermissionHandling.IGNORE);
    }

    @Override
    public void setLastModifiedTime(FileTime lastModifiedTime) {
        archiver.setLastModifiedTime(Objects.requireNonNull(lastModifiedTime, "lastModifiedTime"));
    }

    @Override
    public void setFilenameComparator(Comparator<String> filenameComparator) {
        archiver.setFilenameComparator(Objects.requireNonNull(filenameComparator, "filenameComparator"));
    }

    @Override
    public void setOverrideUid(int uid) {
        if (uid < 0) {
            throw new IllegalArgumentException("uid must not be negative");
        }
        archiver.setOverrideUid(uid);
    }

    @Override
    public void setOverrideUserName(String userName) {
        archiver.setOverrideUserName(Objects.requireNonNull(userName, "userName"));
    }

    @Override
    public void setOverrideGid(int gid) {
        if (gid < 0) {
            throw new IllegalArgumentException("gid must not be negative");
        }
        archiver.setOverrideGid(gid);
    }

    @Override
    public void setOverrideGroupName(String groupName) {
        archiver.setOverrideGroupName(Objects.requireNonNull(groupName, "groupName"));
    }

    @Override
    public void setUmask(FilePermissions permissions) {
        archiver.setUmask(toMode(permissions));
    }

    
	@Override
	public void configureReproducibleBuild(Consumer<ReproducibleBuildConfigurer> spec) {
		ReproducibleBuildConfigurer configurer = new DefaultReproducibleBuildConfigurer(archiver);
		spec.accept(configurer);
	}

    private static int toMode(FilePermissions filePermissions) {
    	if(filePermissions instanceof PosixPermissions permissions) {
        	int mode = 0;
            for (PosixFilePermission permission :
                    Objects.requireNonNull(permissions, "unixPermissions").permissions()) {
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
    	} else if(filePermissions instanceof ModePermissions permissions) {
    		return permissions.mode();
    	} else {
    		throw new IncompatibleClassChangeError();
    	}
    }

}
