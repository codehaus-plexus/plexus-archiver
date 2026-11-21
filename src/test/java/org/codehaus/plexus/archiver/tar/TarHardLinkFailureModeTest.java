/*
 * Copyright The Plexus developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.tar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.HardLinkFailureMode;
import org.codehaus.plexus.archiver.SymlinkFailureMode;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarHardLinkFailureModeTest {
    @TempDir
    Path temp;

    /** Simulates refused native links without requiring a particular operating system or filesystem. */
    private static class RefusingTar extends TarUnArchiver {
        boolean unsupported;
        int attempts;

        RefusingTar(Path source, Path output) {
            super(source.toFile());
            setDestDirectory(output.toFile());
        }

        @Override
        protected void createHardLink(Path link, Path existing) throws IOException {
            attempts++;
            if (unsupported) {
                throw new UnsupportedOperationException("test filesystem");
            }
            throw new AccessDeniedException(link.toString(), existing.toString(), "test refusal");
        }
    }

    /** Writes regular files, directories, or hard links denoted by an arrow before their target. */
    private void entry(TarArchiveOutputStream out, String name, String value, long time) throws IOException {
        boolean symlink = value.startsWith("~>");
        boolean link = value.startsWith("->") || symlink;
        TarArchiveEntry entry = new TarArchiveEntry(
                name,
                symlink
                        ? TarConstants.LF_SYMLINK
                        : link
                                ? TarConstants.LF_LINK
                                : name.endsWith("/") ? TarConstants.LF_DIR : TarConstants.LF_NORMAL);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        entry.setModTime(time);
        entry.setMode(name.endsWith("/") ? 0755 : 0640);
        if (link) {
            entry.setLinkName(value.substring(2));
        } else if (!entry.isDirectory()) {
            entry.setSize(bytes.length);
        }
        out.putArchiveEntry(entry);
        if (!link && !entry.isDirectory()) {
            out.write(bytes);
        }
        out.closeArchiveEntry();
    }

    private Path archive(String... entries) throws IOException {
        Path source = Files.createTempFile(temp, "links", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            for (int i = 0; i < entries.length; i += 2) {
                entry(out, entries[i], entries[i + 1], 1234567000000L);
            }
        }
        return source;
    }

    private Path output() throws IOException {
        return Files.createTempDirectory(temp, "output");
    }

    private void assertNoStaging(Path output) throws IOException {
        try (var paths = Files.walk(output)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".plexus-link-")));
        }
    }

    private void requireHardLinks() throws IOException {
        Path source = Files.writeString(temp.resolve("capability-file"), "probe");
        try {
            Files.createLink(temp.resolve("capability-link"), source);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Hard links unavailable: " + e);
        }
    }

    @Test
    void defaultsToCopyIndependentlyOfSymlinkMode() {
        TarUnArchiver extractor = new TarUnArchiver();
        assertEquals(HardLinkFailureMode.COPY_TARGET, extractor.getHardLinkFailureMode());
        extractor.setSymlinkFailureMode(SymlinkFailureMode.FAIL);
        assertEquals(HardLinkFailureMode.COPY_TARGET, extractor.getHardLinkFailureMode());
        assertThrows(NullPointerException.class, () -> extractor.setHardLinkFailureMode(null));
        assertEquals(HardLinkFailureMode.COPY_TARGET, extractor.getHardLinkFailureMode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void copiesFinalContentsAfterTheEntryLoop(boolean unsupported) throws Exception {
        Path output = output();
        Path source = archive("file", "old", "alias", "->file", "file", "new", "after", "done");
        RefusingTar extractor = new RefusingTar(source, output) {
            @Override
            protected void onEntryExtracted(Path destination) {
                super.onEntryExtracted(destination);
                if (destination.getFileName().toString().equals("after")) {
                    assertFalse(Files.exists(output.resolve("alias")), "Copy must wait for the entry loop to finish");
                }
            }
        };
        extractor.unsupported = unsupported;
        extractor.extract();
        assertEquals(1, extractor.attempts);
        assertEquals("new", Files.readString(output.resolve("alias")));
        assertFalse(Files.isSameFile(output.resolve("file"), output.resolve("alias")));
        assertEquals(
                Files.getLastModifiedTime(output.resolve("file")), Files.getLastModifiedTime(output.resolve("alias")));
        if (Files.getFileStore(output).supportsFileAttributeView("posix")) {
            assertEquals(
                    Files.getPosixFilePermissions(output.resolve("file")),
                    Files.getPosixFilePermissions(output.resolve("alias")));
        }
        assertNoStaging(output);
    }

    @ParameterizedTest
    @EnumSource(TarUnArchiver.UntarCompressionMethod.class)
    void resolvesMappedChainsWithEveryDecoder(TarUnArchiver.UntarCompressionMethod method) throws Exception {
        Path source = archive("file", "payload", "alias", "->file", "another", "->alias");
        if (method != TarUnArchiver.UntarCompressionMethod.NONE) {
            String algorithm =
                    switch (method) {
                        case GZIP -> "gz";
                        case BZIP2 -> "bzip2";
                        case SNAPPY -> "snappy-framed";
                        case XZ -> "xz";
                        case ZSTD -> "zstd";
                        default -> throw new IllegalArgumentException();
                    };
            Path compressed = Files.createTempFile(temp, "compressed", ".tar");
            try (var out = new CompressorStreamFactory()
                    .createCompressorOutputStream(algorithm, Files.newOutputStream(compressed))) {
                Files.copy(source, out);
            }
            source = compressed;
        }
        Path output = output();
        RefusingTar extractor = new RefusingTar(source, output);
        extractor.setCompression(method);
        extractor.setFileMappers(new FileMapper[] {name -> "mapped/" + name});
        extractor.extract();
        assertEquals(1, extractor.attempts, "A dependent queued alias has no native target yet");
        assertEquals("payload", Files.readString(output.resolve("mapped/alias")));
        assertEquals("payload", Files.readString(output.resolve("mapped/another")));
        assertFalse(Files.isSameFile(output.resolve("mapped/alias"), output.resolve("mapped/another")));
        assertNoStaging(output);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitFailStopsAtTheRefusedLink(boolean unsupported) throws Exception {
        Path output = output();
        RefusingTar extractor = new RefusingTar(archive("file", "payload", "alias", "->file", "after", "done"), output);
        extractor.unsupported = unsupported;
        extractor.setHardLinkFailureMode(HardLinkFailureMode.FAIL);
        ArchiverException failure = assertThrows(ArchiverException.class, extractor::extract);
        Class<? extends Throwable> expected =
                unsupported ? UnsupportedOperationException.class : AccessDeniedException.class;
        assertInstanceOf(expected, failure.getCause().getCause());
        assertFalse(Files.exists(output.resolve("alias")));
        assertFalse(Files.exists(output.resolve("after")));
        assertNoStaging(output);
    }

    @Test
    void explicitSkipContinuesWithoutCopying() throws Exception {
        Path output = output();
        RefusingTar extractor = new RefusingTar(archive("file", "payload", "alias", "->file", "after", "done"), output);
        extractor.setHardLinkFailureMode(HardLinkFailureMode.SKIP);
        extractor.extract();
        assertFalse(Files.exists(output.resolve("alias")));
        assertEquals("done", Files.readString(output.resolve("after")));
        assertNoStaging(output);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void honorsOverwritePolicy(boolean overwrite) throws Exception {
        Path output = output();
        Files.writeString(output.resolve("alias"), "existing");
        RefusingTar extractor = new RefusingTar(archive("file", "payload", "alias", "->file"), output);
        extractor.setOverwrite(overwrite);
        extractor.extract();
        assertEquals(overwrite ? "payload" : "existing", Files.readString(output.resolve("alias")));
        assertEquals(overwrite ? 1 : 0, extractor.attempts);
        assertNoStaging(output);
    }

    @Test
    void laterSuccessfulWriteCancelsTheQueuedCopy() throws Exception {
        Path output = output();
        RefusingTar extractor =
                new RefusingTar(archive("file", "payload", "alias", "->file", "alias", "later"), output);
        extractor.extract();
        assertEquals("later", Files.readString(output.resolve("alias")));
        assertNoStaging(output);
    }

    @Test
    void dependentCopyUsesTheReplacementOfACanceledTarget() throws Exception {
        Path output = output();
        new RefusingTar(
                        archive("file", "payload", "alias", "->file", "another", "->alias", "alias", "replacement"),
                        output)
                .extract();
        assertEquals("replacement", Files.readString(output.resolve("another")));
        assertNoStaging(output);
    }

    @Test
    void occupiedSymlinkEntryDoesNotCancelTheQueuedCopy() throws Exception {
        Path output = output();
        Files.writeString(output.resolve("alias"), "existing");
        new RefusingTar(archive("file", "payload", "alias", "->file", "alias", "~>file"), output).extract();
        assertFalse(Files.isSymbolicLink(output.resolve("alias")));
        assertEquals("payload", Files.readString(output.resolve("alias")));
        assertNoStaging(output);
    }

    @Test
    void laterNativeLinkCancelsTheQueuedCopy() throws Exception {
        requireHardLinks();
        Path output = output();
        RefusingTar extractor =
                new RefusingTar(
                        archive("file", "payload", "alias", "->file", "other", "replacement", "alias", "->other"),
                        output) {
                    @Override
                    protected void createHardLink(Path link, Path existing) throws IOException {
                        if (attempts == 0) {
                            super.createHardLink(link, existing);
                        } else {
                            attempts++;
                            Files.createLink(link, existing);
                        }
                    }
                };
        extractor.extract();
        assertEquals(2, extractor.attempts);
        assertEquals("replacement", Files.readString(output.resolve("alias")));
        assertTrue(Files.isSameFile(output.resolve("alias"), output.resolve("other")));
        assertNoStaging(output);
    }

    @Test
    void replacingAnOutputDoesNotModifyItsOtherAliases() throws Exception {
        requireHardLinks();
        Path output = output();
        Path alias = Files.writeString(output.resolve("alias"), "existing");
        Path retained = Files.createLink(temp.resolve("retained"), alias);
        new RefusingTar(archive("file", "payload", "alias", "->file"), output).extract();
        assertEquals("existing", Files.readString(retained));
        assertEquals("payload", Files.readString(alias));
        assertFalse(Files.isSameFile(alias, retained));
        assertNoStaging(output);
    }

    @Test
    void laterSkippedEntryDoesNotCancelTheQueuedCopy() throws Exception {
        Path source = Files.createTempFile(temp, "skipped", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "file", "payload", 1234567000000L);
            entry(out, "alias", "->file", 1234567000000L);
            entry(out, "alias", "older", 1000);
        }
        Path output = output();
        Files.writeString(output.resolve("alias"), "existing");
        Files.setLastModifiedTime(output.resolve("alias"), FileTime.fromMillis(2000));
        RefusingTar extractor = new RefusingTar(source, output);
        extractor.setOverwrite(false);
        extractor.extract();
        assertEquals("payload", Files.readString(output.resolve("alias")));
    }

    @Test
    void latestQueuedOccurrenceWins() throws Exception {
        Path output = output();
        new RefusingTar(archive("first", "old", "second", "new", "alias", "->first", "alias", "->second"), output)
                .extract();
        assertEquals("new", Files.readString(output.resolve("alias")));
    }

    @Test
    void unresolvedCopyDependenciesAreSkipped() throws Exception {
        Path output = output();
        new RefusingTar(archive("file", "payload", "a", "->file", "b", "->a", "a", "->b"), output).extract();
        assertFalse(Files.exists(output.resolve("a")));
        assertFalse(Files.exists(output.resolve("b")));
        assertNoStaging(output);
    }

    @Test
    void rechecksOverwriteEligibilityBeforeCopying() throws Exception {
        Path output = output();
        Files.writeString(output.resolve("alias"), "existing");
        Files.setLastModifiedTime(output.resolve("alias"), FileTime.fromMillis(1000));
        RefusingTar extractor =
                new RefusingTar(archive("file", "payload", "alias", "->file", "after", "done"), output) {
                    @Override
                    protected void onEntryExtracted(Path destination) {
                        super.onEntryExtracted(destination);
                        if (destination.getFileName().toString().equals("after")) {
                            try {
                                Files.setLastModifiedTime(output.resolve("alias"), FileTime.fromMillis(2000000000000L));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                };
        extractor.setOverwrite(false);
        extractor.extract();
        assertEquals(1, extractor.attempts);
        assertEquals("existing", Files.readString(output.resolve("alias")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void excludedTargetsMustExistBeforeFallback(boolean existing) throws Exception {
        Path output = output();
        if (existing) {
            Files.writeString(output.resolve("file"), "existing");
        }
        RefusingTar extractor = new RefusingTar(archive("file", "archived", "alias", "->file"), output);
        extractor.setFileSelectors(new FileSelector[] {file -> file.getName().equals("alias")});
        if (existing) {
            extractor.extract();
            assertEquals("existing", Files.readString(output.resolve("alias")));
        } else {
            assertThrows(ArchiverException.class, extractor::extract);
            assertEquals(0, extractor.attempts);
            assertFalse(Files.exists(output.resolve("alias")));
        }
        assertNoStaging(output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "../outside", "/outside", "alias"})
    void invalidRelationshipsDoNotUseFallback(String target) throws Exception {
        Path output = output();
        RefusingTar extractor = new RefusingTar(archive("file", "payload", "alias", "->" + target), output);
        assertThrows(ArchiverException.class, extractor::extract);
        assertEquals(0, extractor.attempts);
        assertFalse(Files.exists(output.resolve("alias")));
        assertNoStaging(output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "directory", "symlink"})
    void skipsTargetsThatBecomeNonRegular(String kind) throws Exception {
        Path output = output();
        Path outside = Files.writeString(temp.resolve("outside"), "secret");
        if (kind.equals("symlink")) {
            try {
                Files.createSymbolicLink(temp.resolve("capability"), outside);
            } catch (IOException | UnsupportedOperationException e) {
                assumeTrue(false, "Symbolic links unavailable: " + e);
            }
        }
        RefusingTar extractor =
                new RefusingTar(archive("file", "payload", "alias", "->file", "after", "done"), output) {
                    @Override
                    protected void onEntryExtracted(Path destination) {
                        super.onEntryExtracted(destination);
                        if (destination.getFileName().toString().equals("after")) {
                            try {
                                Files.delete(output.resolve("file"));
                                if (kind.equals("directory")) {
                                    Files.createDirectory(output.resolve("file"));
                                } else if (kind.equals("symlink")) {
                                    Files.createSymbolicLink(output.resolve("file"), outside);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                };
        extractor.extract();
        assertFalse(Files.exists(output.resolve("alias")));
        assertEquals("secret", Files.readString(outside));
        assertNoStaging(output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"outside", "inside-strict", "inside-default", "destination"})
    void rechecksSymlinkedParentsBeforeCopying(String kind) throws Exception {
        Path output = output();
        Path other = kind.equals("outside") || kind.equals("destination")
                ? Files.createDirectory(temp.resolve("outside"))
                : Files.createDirectory(output.resolve("inside"));
        Files.writeString(other.resolve("file"), "replacement");
        try {
            Files.createSymbolicLink(temp.resolve("capability"), other);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        String alias = kind.equals("destination") ? "aliases/link" : "alias";
        RefusingTar extractor =
                new RefusingTar(archive("parent/file", "payload", alias, "->parent/file", "after", "done"), output) {
                    @Override
                    protected void onEntryExtracted(Path destination) {
                        super.onEntryExtracted(destination);
                        if (destination.getFileName().toString().equals("after")) {
                            try {
                                String parent = kind.equals("destination") ? "aliases" : "parent";
                                Files.move(output.resolve(parent), temp.resolve("original-parent"));
                                Files.createSymbolicLink(output.resolve(parent), other);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                };
        extractor.setFailOnSymlinkTraversal(kind.equals("inside-strict"));
        extractor.extract();
        if (kind.equals("inside-default")) {
            assertEquals("replacement", Files.readString(output.resolve(alias)));
        } else {
            assertFalse(Files.exists(output.resolve(alias)));
        }
        assertEquals("replacement", Files.readString(other.resolve("file")));
        assertFalse(Files.exists(other.resolve("link")));
        assertNoStaging(output);
        assertNoStaging(other);
    }

    @Test
    void nativeRenameFailureDoesNotQueueACopy() throws Exception {
        requireHardLinks();
        Path output = output();
        TarUnArchiver extractor =
                new TarUnArchiver(archive("file", "payload", "alias", "->file", "after", "done")
                        .toFile()) {
                    @Override
                    protected void createHardLink(Path link, Path existing) throws IOException {
                        Files.createLink(link, existing);
                        Files.createDirectory(output.resolve("alias"));
                        Files.writeString(output.resolve("alias/child"), "occupied");
                    }
                };
        extractor.setDestDirectory(output.toFile());
        assertThrows(ArchiverException.class, extractor::extract);
        assertEquals("occupied", Files.readString(output.resolve("alias/child")));
        assertFalse(Files.exists(output.resolve("after")));
        assertNoStaging(output);
    }

    @Test
    void copyFailurePreservesExistingOutputAndCleansStaging() throws Exception {
        Path output = output();
        assumeTrue(Files.getFileStore(output).supportsFileAttributeView("posix"), "POSIX read permissions required");
        Path probe = Files.writeString(temp.resolve("read-permission-probe"), "probe");
        try {
            Files.setPosixFilePermissions(probe, Set.of());
            assumeTrue(!Files.isReadable(probe), "Process bypasses read permissions");
        } finally {
            Files.setPosixFilePermissions(probe, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        }
        Files.writeString(output.resolve("alias"), "existing");
        RefusingTar extractor =
                new RefusingTar(archive("file", "payload", "alias", "->file", "after", "done"), output) {
                    @Override
                    protected void onEntryExtracted(Path destination) {
                        super.onEntryExtracted(destination);
                        if (destination.getFileName().toString().equals("after")) {
                            try {
                                Files.setPosixFilePermissions(output.resolve("file"), Set.of());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                };
        try {
            assertThrows(ArchiverException.class, extractor::extract);
            assertEquals("existing", Files.readString(output.resolve("alias")));
            assertNoStaging(output);
        } finally {
            if (Files.exists(output.resolve("file"))) {
                Files.setPosixFilePermissions(
                        output.resolve("file"), java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        }
    }

    @Test
    void abortedExtractionDoesNotDrainOrRetainItsQueue() throws Exception {
        Path output = output();
        RefusingTar extractor =
                new RefusingTar(archive("file", "payload", "alias", "->file", "bad", "->missing"), output);
        assertThrows(ArchiverException.class, extractor::extract);
        assertFalse(Files.exists(output.resolve("alias")));
        extractor.setSourceFile(archive("after", "done").toFile());
        extractor.extract();
        assertEquals("done", Files.readString(output.resolve("after")));
        assertFalse(Files.exists(output.resolve("alias")));
        assertNoStaging(output);
    }
}
