package org.codehaus.plexus.archivers.config;

import java.util.Objects;

/**
 * Specifies how permissions are selected for emitted archive entries.
 *
 * @since 5.0.0
 */
public sealed interface DirectoryMode
        permits InheritedDirectoryMode, OverrideDirectoryMode, FallbackDirectoryMode {

    /**
     * Preserve source permissions when available.
     * Otherwise, use the archiver's built-in fallback.
     *
     * This is the default policy.
     */
    DirectoryMode INHERIT = new InheritedDirectoryMode();

    /**
     * Replace source permissions with the supplied permissions.
     */
    static DirectoryMode override(FilePermissions permissions) {
        return new OverrideDirectoryMode(permissions);
    }

    /**
     * Preserve source permissions when available.
     * Use the supplied permissions only when the source has none.
     */
    static DirectoryMode fallback(FilePermissions permissions) {
        return new FallbackDirectoryMode(permissions);
    }
}

record InheritedDirectoryMode() implements DirectoryMode {}

record OverrideDirectoryMode(FilePermissions permissions) implements DirectoryMode {
    OverrideDirectoryMode {
        Objects.requireNonNull(permissions, "permissions");
    }
}

record FallbackDirectoryMode(FilePermissions permissions) implements DirectoryMode {
	FallbackDirectoryMode {
        Objects.requireNonNull(permissions, "permissions");
    }
}