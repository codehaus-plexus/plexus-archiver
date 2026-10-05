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

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TarArchivedFileSetCleanupTest extends TestSupport {
    @TempDir
    Path temp;

    /** A dedicated JVM isolates the real temporary cache without changing global settings in the test suite. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void conversionsReleasePayloadCaches(boolean failure) throws Exception {
        Path source = temp.resolve("source.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "payload", null);
            TarHardLinkTest.entry(out, "alias", "", "data");
            if (failure) {
                TarHardLinkTest.entry(out, "invalid", "", "absent");
            }
        }
        Path cache = Files.createDirectory(temp.resolve("cache"));
        Path log = temp.resolve("conversion.log");
        String java = Path.of(System.getProperty("java.home"), "bin", File.separatorChar == '\\' ? "java.exe" : "java")
                .toString();
        Process process = new ProcessBuilder(
                        java,
                        "-Djava.io.tmpdir=" + cache,
                        "-cp",
                        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                        TarArchivedFileSetCleanupTest.class.getName(),
                        source.toString(),
                        Boolean.toString(failure))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "conversion process timed out");
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            process.destroyForcibly();
        }
        assertNoPayloadCaches(cache);
    }

    /** Exercises the public archived-file-set API repeatedly, including failure after a cached alias was read. */
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]);
        boolean failure = Boolean.parseBoolean(args[1]);
        TarArchivedFileSetCleanupTest test = new TarArchivedFileSetCleanupTest();
        test.setUp();
        try {
            Archiver archiver = test.lookup(Archiver.class, "zip");
            for (int i = 0; i < 2; i++) {
                DefaultArchivedFileSet set = new DefaultArchivedFileSet(source.toFile());
                set.setIncludes(new String[] {"alias", "invalid"});
                archiver.addArchivedFileSet(set);
                Path output = source.resolveSibling("converted-" + i + ".zip");
                archiver.setDestFile(output.toFile());
                if (failure) {
                    assertThrows(UncheckedIOException.class, archiver::createArchive);
                } else {
                    archiver.createArchive();
                    try (var zip = new java.util.zip.ZipFile(output.toFile());
                            var contents = zip.getInputStream(zip.getEntry("alias"))) {
                        assertEquals("payload", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
                // Callers cannot close the internally created collection; createArchive must release its cache.
                assertNoPayloadCaches(Path.of(System.getProperty("java.io.tmpdir")));
            }
        } finally {
            test.tearDown();
        }
    }

    /** Checks ownership of TAR caches independently of other libraries' temporary files. */
    private static void assertNoPayloadCaches(Path directory) throws java.io.IOException {
        try (var paths = Files.list(directory)) {
            assertEquals(
                    0,
                    paths.filter(path -> path.getFileName().toString().startsWith("plexus-tar-"))
                            .count());
        }
    }
}
