/*
 * Copyright 2026 The plexus developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.tar;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/** Owns temporary payloads, storing each data-bearing occurrence only once. */
final class TarPayloads implements Closeable {
    private final Path directory;
    private final Map<TarArchiveEntry, Path> files = new IdentityHashMap<>();

    /** Creates a private cache only when a caller requests hard-link contents. */
    TarPayloads() throws IOException {
        directory = Files.createTempDirectory("plexus-tar-");
    }

    /** Copies one logical payload, retaining it only after the copy and stream closures succeed. */
    Path add(TarFile archive, TarArchiveEntry entry) throws IOException {
        Path existing = files.get(entry);
        if (existing != null) {
            return existing;
        }
        Path target = Files.createTempFile(directory, "payload-", "");
        boolean complete = false;
        try {
            try (InputStream input = archive.rawContents(entry);
                    OutputStream output = Files.newOutputStream(target)) {
                input.transferTo(output);
            }
            files.put(entry, target);
            complete = true;
            return target;
        } finally {
            // Even a stream-close failure must not leave an untracked temporary payload.
            if (!complete) {
                Files.deleteIfExists(target);
            }
        }
    }

    /** Removes anchors on success and failure without traversing extracted output directories. */
    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (Path path : files.values()) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        try {
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
