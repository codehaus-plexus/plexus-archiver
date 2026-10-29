package org.codehaus.plexus.archivers.config;

import java.util.Objects;

public sealed interface PermissionPolicy
        permits IgnorePermissionPolicy,
                UmaskPermissionPolicy,
                EntryModesPermissionPolicy {

    PermissionPolicy IGNORE = new IgnorePermissionPolicy();

    static PermissionPolicy umask(FilePermissions mask) {
        return new UmaskPermissionPolicy(mask);
    }

    static PermissionPolicy modes(
            FileMode files, DirectoryMode directories) {
        return new EntryModesPermissionPolicy(files, directories);
    }
}

final class IgnorePermissionPolicy implements PermissionPolicy {}

final class UmaskPermissionPolicy implements PermissionPolicy {
    private final FilePermissions mask;

    UmaskPermissionPolicy(FilePermissions mask) {
        this.mask = Objects.requireNonNull(mask, "mask");
    }

    FilePermissions mask() {
        return mask;
    }
}

final class EntryModesPermissionPolicy implements PermissionPolicy {
    private final FileMode files;
    private final DirectoryMode directories;

    EntryModesPermissionPolicy(
            FileMode files, DirectoryMode directories) {
        this.files = Objects.requireNonNull(files, "files");
        this.directories =
                Objects.requireNonNull(directories, "directories");
    }

    FileMode files() {
        return files;
    }

    DirectoryMode directories() {
        return directories;
    }
}