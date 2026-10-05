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
package org.codehaus.plexus.archiver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Changes directories between entries to ensure cached spellings never replace filesystem validation. */
class ExtractionPathCacheTest {
    @TempDir
    Path temp;

    @FunctionalInterface
    private interface Entries {
        void extract(Extractor extractor) throws IOException;
    }

    private static class Extractor extends AbstractUnArchiver {
        Entries entries;

        @Override
        protected void execute() {
            try {
                entries.extract(this);
            } catch (IOException e) {
                throw new ArchiverException("Cannot extract fixture", e);
            }
        }

        @Override
        protected void execute(String path, File outputDirectory) {
            execute();
        }

        void write(String name) throws IOException {
            extractFile(
                    getSourceFile(),
                    getDestDirectory(),
                    new ByteArrayInputStream(new byte[] {1}),
                    name,
                    new Date(),
                    false,
                    null,
                    null,
                    null);
        }

        void run(boolean explicitDirectory) {
            if (explicitDirectory) {
                extract("", getDestDirectory());
            } else {
                extract();
            }
        }
    }

    private Extractor extractor(Path root) throws IOException {
        Extractor extractor = new Extractor();
        extractor.setDestDirectory(root.toFile());
        extractor.setSourceFile(Files.createTempFile(temp, "source", ".archive").toFile());
        return extractor;
    }

    private void symlink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void observesDirectoryReplacedBySymlinkWithinExtraction(boolean explicitDirectory) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Extractor extractor = extractor(root);
        extractor.entries = current -> {
            current.write("parent/first");
            Files.move(root.resolve("parent"), root.resolve("saved"));
            symlink(root.resolve("parent"), outside);
            assertThrows(ArchiverException.class, () -> current.write("parent/new/file"));
            assertArrayEquals(new byte[] {1}, Files.readAllBytes(root.resolve("saved/first")));
        };
        extractor.run(explicitDirectory);
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void observesPreviouslyMissingParent(boolean explicitDirectory) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Extractor extractor = extractor(root);
        extractor.entries = current -> {
            current.write("missing/../first");
            symlink(root.resolve("missing"), outside);
            assertThrows(ArchiverException.class, () -> current.write("missing/new/file"));
        };
        extractor.run(explicitDirectory);
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void canReuseExtractorAfterFailureAndSuccess(boolean explicitDirectory) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path first = Files.createDirectory(root.resolve("first"));
        Path second = Files.createDirectory(root.resolve("second"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Path link = root.resolve("link");
        symlink(link, first);
        Extractor extractor = extractor(root);
        extractor.entries = current -> {
            current.write("link/before");
            Files.delete(link);
            symlink(link, outside);
            current.write("link/rejected");
        };
        assertThrows(ArchiverException.class, () -> extractor.run(explicitDirectory));
        assertTrue(Files.exists(first.resolve("before")));

        Files.delete(link);
        symlink(link, second);
        extractor.entries = current -> current.write("link/after");
        extractor.run(explicitDirectory);
        assertTrue(Files.exists(second.resolve("after")));

        Files.delete(link);
        symlink(link, first);
        extractor.entries = current -> current.write("link/reused");
        extractor.run(explicitDirectory);
        assertTrue(Files.exists(first.resolve("reused")));
        assertFalse(Files.exists(second.resolve("reused")));
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }
}
