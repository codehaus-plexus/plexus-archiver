package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.Objects;

record DotFileDirectoryFinalizationPolicy(Path directory) implements ArchiverFinalizationPolicy {
    DotFileDirectoryFinalizationPolicy {
        Objects.requireNonNull(directory, "directory");
    }
}