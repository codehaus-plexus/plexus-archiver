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

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

/**
 * File permissions applied to archive entries or used as an archive umask.
 *
 * @since 5.0.0
 */
public abstract sealed class FilePermissions permits PosixPermissions, ModePermissions {

    FilePermissions() {}

    public static FilePermissions of(Set<PosixFilePermission> permissions) {
        return new PosixPermissions(permissions);
    }

    public static FilePermissions parse(String pattern) {
        return new PosixPermissions(PosixFilePermissions.fromString(pattern));
    }
    
    public static FilePermissions ofMode(int mode) {
        return new ModePermissions(mode);
    }
}


final class PosixPermissions extends FilePermissions {
    final Set<PosixFilePermission> permissions;

    PosixPermissions(Set<PosixFilePermission> permissions) {
        this.permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
    }

    public Set<PosixFilePermission> permissions() {
        return permissions;
    }
}
final class ModePermissions extends FilePermissions {
    final int mode;

    public ModePermissions(int mode) {
    	this.mode = mode;
	}
    
    int mode() {
    	return mode;
    }
}

