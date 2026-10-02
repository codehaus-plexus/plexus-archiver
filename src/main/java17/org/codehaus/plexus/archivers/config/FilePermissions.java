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
 * Represents archive file permissions in a form suitable for archiver configuration.
 * <p>
 * This type provides a small, public abstraction over the underlying permission representation so consumers can
 * configure archive entry permissions using either symbolic POSIX-style permissions or numeric mode values.
 * </p>
 *
 * @since 5.0.0
 */
public sealed abstract class FilePermissions permits PosixPermissions, ModePermissions {

    FilePermissions() {}

    /**
     * Creates permissions from a set of POSIX file permissions.
     *
     * @param permissions the POSIX permissions
     * @return a permission representation for archiver configuration
     */
    public static FilePermissions of(Set<PosixFilePermission> permissions) {
        return new PosixPermissions(permissions);
    }

    /**
     * Parses a POSIX symbolic permission string.
     * <p>
     * The expected format is the standard symbolic representation understood by
     * {@link java.nio.file.attribute.PosixFilePermissions#fromString(String)}, such as
     * {@code rw-r--r--} or {@code rwxr-xr-x}.
     * </p>
     *
     * @param symbolicPermissions the POSIX symbolic permission string
     * @return a permission representation for archiver configuration
     * @throws IllegalArgumentException if the string is not a valid POSIX symbolic permission value
     */
    public static FilePermissions parse(String symbolicPermissions) {
        return new PosixPermissions(PosixFilePermissions.fromString(symbolicPermissions));
    }
    
    /**
     * Creates permissions from a numeric archive mode.
     * <p>
     * The mode should be expressed using the familiar octal form, for example {@code 0644} or {@code 0755}.
     * </p>
     *
     * @param mode the numeric archive mode
     * @return a permission representation for archiver configuration
     */
    public static FilePermissions ofMode(int mode) {
        return new ModePermissions(mode);
    }
}

/**
 * POSIX-style permissions represented as a set of {@link PosixFilePermission} values.
 * <p>
 * This representation is useful when consumers already have permissions in Java NIO form.
 * </p>
 */
final class PosixPermissions extends FilePermissions {
    final Set<PosixFilePermission> permissions;

    PosixPermissions(Set<PosixFilePermission> permissions) {
        this.permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
    }

    public Set<PosixFilePermission> permissions() {
        return permissions;
    }
}

/**
 * Numeric archive permissions represented as a Unix-style mode value.
 * <p>
 * The mode is typically expressed using the familiar octal notation, such as {@code 0644} or {@code 0755}.
 * This is the most convenient form for archive configuration because archive tools usually work with mode bits
 * rather than full filesystem permission models.
 * </p>
 */
final class ModePermissions extends FilePermissions {
    final int mode;

    public ModePermissions(int mode) {
    	this.mode = mode;
	}
    
    int mode() {
    	return mode;
    }
}

