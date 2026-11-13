package org.codehaus.plexus.archivers.config;

import java.util.Objects;

/**
 * Specifies how permissions are selected for emitted archive entries.
 *
 * @since 5.0.0
 */
public sealed interface FileMode
        permits InheritedFileMode, OverrideFileMode, FallbackFileMode {

    /**
     * Preserve source permissions when available.
     * Otherwise, use the archiver's built-in fallback.
     *
     * This is the default policy.
     */
    FileMode INHERIT = new InheritedFileMode();

    /**
     * Replace source permissions with the supplied permissions.
     */
    static FileMode override(FilePermissions permissions) {
        return new OverrideFileMode(permissions);
    }

    /**
     * Preserve source permissions when available.
     * Use the supplied permissions only when the source has none.
     */
    static FileMode fallback(FilePermissions permissions) {
        return new FallbackFileMode(permissions);
    }
}

record InheritedFileMode() implements FileMode {}

record OverrideFileMode(FilePermissions permissions) implements FileMode {
    OverrideFileMode {
        Objects.requireNonNull(permissions, "permissions");
    }
}

record FallbackFileMode(FilePermissions permissions) implements FileMode {
    FallbackFileMode {
        Objects.requireNonNull(permissions, "permissions");
    }
}