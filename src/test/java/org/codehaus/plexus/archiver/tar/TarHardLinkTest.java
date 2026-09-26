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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarHardLinkTest {
    @TempDir
    Path temp;

    /** Verifies that the test filesystem supports links without excluding an entire operating system. */
    void requireHardLinks() throws IOException {
        Path file = Files.createTempFile(temp, "capability", "");
        try {
            Files.createLink(temp.resolve(file.getFileName() + ".link"), file);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Hard links unavailable: " + e);
        }
    }

    /** Writes a data member or a zero-payload hard-link header for independently constructed fixtures. */
    static void entry(TarArchiveOutputStream out, String name, String contents, String target) throws IOException {
        TarArchiveEntry entry =
                new TarArchiveEntry(name, target == null ? TarConstants.LF_NORMAL : TarConstants.LF_LINK);
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        entry.setModTime(1234567000000L);
        entry.setMode(0644);
        if (target == null) {
            entry.setSize(bytes.length);
        } else {
            entry.setLinkName(target);
        }
        out.putArchiveEntry(entry);
        if (target == null) {
            out.write(bytes);
        }
        out.closeArchiveEntry();
    }

    /** Creates a target and two aliases, optionally putting the aliases before their data. */
    Path archive(boolean forward) throws IOException {
        Path archive = Files.createTempFile(temp, "links", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            if (!forward) {
                entry(out, "file", "archive content", null);
            }
            entry(out, "link", "", "file");
            entry(out, "another", "", "link");
            if (forward) {
                entry(out, "file", "archive content", null);
            }
        }
        return archive;
    }

    /** Configures a standalone extractor without requiring container lookup. */
    TarUnArchiver unarchiver(Path archive, Path output) {
        TarUnArchiver unarchiver = new TarUnArchiver(archive.toFile());
        unarchiver.setDestDirectory(output.toFile());
        return unarchiver;
    }

    /** Covers the ordering and mapping concerns from PR 286, including a chain of links. */
    @Test
    void extractsBackwardLinksThroughMappers() throws Exception {
        requireHardLinks();
        Path output = Files.createDirectory(temp.resolve("mapped"));
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setFileMappers(new FileMapper[] {name -> "renamed/" + name});
        unarchiver.extract();
        assertEquals("archive content", Files.readString(output.resolve("renamed/link")));
        assertTrue(Files.isSameFile(output.resolve("renamed/file"), output.resolve("renamed/link")));
        assertTrue(Files.isSameFile(output.resolve("renamed/file"), output.resolve("renamed/another")));
    }

    /** An excluded target uses the existing mapped destination, as command-line tar does. */
    @Test
    void selectedAliasesUseExistingTarget() throws Exception {
        requireHardLinks();
        Path output = Files.createDirectory(temp.resolve("selected"));
        Files.writeString(output.resolve("file"), "unrelated existing content");
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("file")});
        unarchiver.extract();
        assertEquals("unrelated existing content", Files.readString(output.resolve("link")));
        assertEquals("unrelated existing content", Files.readString(output.resolve("file")));
        assertTrue(Files.isSameFile(output.resolve("file"), output.resolve("link")));
        assertTrue(Files.isSameFile(output.resolve("link"), output.resolve("another")));
    }

    /** The archive-resource view must expose logical bytes without skipping the next member. */
    @Test
    void resourceContentsResolveLinksWithoutChangingEnumeration() throws Exception {
        TarFile file = new TarFile(archive(false).toFile());
        try {
            Enumeration<ArchiveEntry> entries = file.getEntries();
            assertTrue(entries.hasMoreElements());
            assertEquals("file", entries.nextElement().getName());
            TarResource link = new TarResource(file, (TarArchiveEntry) entries.nextElement());
            assertEquals(15, link.getSize());
            try (var contents = link.getContents()) {
                assertEquals("archive content", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertTrue(entries.hasMoreElements());
            assertEquals("another", entries.nextElement().getName());
            assertFalse(entries.hasMoreElements());
        } finally {
            file.close();
        }
    }
    /** A missing excluded target fails without seeking back or creating excluded names. */
    @Test
    void rejectsAliasWhenExcludedTargetIsAbsent() throws Exception {
        Path output = Files.createDirectory(temp.resolve("single"));
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setFileSelectors(new FileSelector[] {file -> file.getName().equals("link")});
        assertThrows(org.codehaus.plexus.archiver.ArchiverException.class, unarchiver::extract);
        try (var files = Files.list(output)) {
            assertEquals(0, files.count());
        }
    }

    /** Replacement must unlink an existing inode instead of modifying its other aliases. */
    @Test
    void overwriteDoesNotModifyExistingAliases() throws Exception {
        requireHardLinks();
        Path output = Files.createDirectory(temp.resolve("overwrite"));
        Files.writeString(output.resolve("link"), "old data");
        Files.createLink(temp.resolve("old-alias"), output.resolve("link"));
        unarchiver(archive(false), output).extract();
        assertEquals("archive content", Files.readString(output.resolve("link")));
        assertEquals("old data", Files.readString(temp.resolve("old-alias")));
        assertTrue(Files.isSameFile(output.resolve("link"), output.resolve("file")));
    }

    /** Overwrite policy retains the current target and links aliases to that same inode. */
    @Test
    void overwritePolicyLinksToRetainedTarget() throws Exception {
        requireHardLinks();
        Path output = Files.createDirectory(temp.resolve("retained"));
        Files.writeString(output.resolve("file"), "newer existing data");
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setOverwrite(false);
        unarchiver.extract();
        assertEquals("newer existing data", Files.readString(output.resolve("file")));
        assertEquals("newer existing data", Files.readString(output.resolve("link")));
        assertTrue(Files.isSameFile(output.resolve("link"), output.resolve("another")));
        assertTrue(Files.isSameFile(output.resolve("link"), output.resolve("file")));
    }

    /** Unsupported linking reports a failure, preserves an existing output, and cleans staging. */
    @Test
    void unsupportedLinksDoNotSilentlyCopy() throws Exception {
        Path output = Files.createDirectory(temp.resolve("unsupported"));
        Files.writeString(output.resolve("link"), "existing");
        TarUnArchiver unarchiver = new TarUnArchiver(archive(false).toFile()) {
            /** Simulates a filesystem without link support on every test platform. */
            @Override
            protected void createHardLink(Path link, Path existing) {
                throw new UnsupportedOperationException("test filesystem");
            }
        };
        unarchiver.setDestDirectory(output.toFile());
        assertThrows(org.codehaus.plexus.archiver.ArchiverException.class, unarchiver::extract);
        assertEquals("existing", Files.readString(output.resolve("link")));
        assertEquals("archive content", Files.readString(output.resolve("file")));
        try (var files = Files.list(output)) {
            assertEquals(2, files.count());
        }
    }

    /** Missing targets, cycles and traversal must fail instead of creating empty or external files. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"missing", "cycle", "../outside", "/outside", "dir", "sym"})
    void rejectsInvalidTargets(String target) throws Exception {
        Path source = Files.createTempFile(temp, "invalid", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            if (target.equals("cycle")) {
                entry(out, "cycle", "", "link");
            } else if (target.equals("dir") || target.equals("sym")) {
                TarArchiveEntry special = new TarArchiveEntry(
                        target, target.equals("dir") ? TarConstants.LF_DIR : TarConstants.LF_SYMLINK);
                if (target.equals("sym")) {
                    special.setLinkName("missing");
                }
                out.putArchiveEntry(special);
                out.closeArchiveEntry();
            }
            entry(out, "link", "", target);
        }
        Path output = Files.createDirectory(temp.resolve("invalid-output"));
        TarUnArchiver unarchiver = unarchiver(source, output);
        assertThrows(org.codehaus.plexus.archiver.ArchiverException.class, unarchiver::extract);
        assertFalse(Files.exists(output.resolve("link")));
    }

    /** A symlinked destination parent cannot redirect staged payloads outside the output tree. */
    @Test
    void rejectsSymlinkEscape() throws Exception {
        Path output = Files.createDirectory(temp.resolve("escape-output"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        try {
            Files.createSymbolicLink(output.resolve("redirect"), outside);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setFileMappers(new FileMapper[] {name -> "redirect/" + name});
        assertThrows(org.codehaus.plexus.archiver.ArchiverException.class, unarchiver::extract);
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    /** Neither an existing output nor an excluded target may be a final symbolic link. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "false, inside", "false, outside", "false, missing",
        "true, inside", "true, outside", "true, missing"
    })
    void rejectsFinalSymlinks(boolean targetLink, String location) throws Exception {
        Path output = Files.createDirectory(temp.resolve("output"));
        Path target = (location.equals("inside") ? output : temp).resolve("real");
        if (!location.equals("missing")) {
            Files.writeString(target, "original");
        }
        Path link = output.resolve(targetLink ? "file" : "link");
        Path retained = output.resolve(targetLink ? "link" : "file");
        Files.writeString(retained, "retained");
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        TarUnArchiver unarchiver = unarchiver(archive(false), output);
        unarchiver.setFileSelectors(new FileSelector[] {file -> file.getName().equals("link")});
        assertThrows(org.codehaus.plexus.archiver.ArchiverException.class, unarchiver::extract);
        assertEquals(target, Files.readSymbolicLink(link));
        assertEquals("retained", Files.readString(retained));
        if (location.equals("missing")) {
            assertFalse(Files.exists(target, LinkOption.NOFOLLOW_LINKS));
        } else {
            assertEquals("original", Files.readString(target));
        }
        try (var files = Files.list(output)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".plexus-link-")));
        }
    }

    /** A duplicate data member does not change an alias bound to its earlier occurrence. */
    @Test
    void preservesDuplicateOccurrencesAndArchiveOrder() throws Exception {
        requireHardLinks();
        Path source = Files.createTempFile(temp, "duplicates", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "file", "old", null);
            entry(out, "old-link", "", "file");
            entry(out, "file", "new", null);
            entry(out, "new-link", "", "file");
        }
        Path output = Files.createDirectory(temp.resolve("duplicates-output"));
        unarchiver(source, output).extract();
        assertEquals("old", Files.readString(output.resolve("old-link")));
        assertEquals("new", Files.readString(output.resolve("new-link")));
        assertTrue(Files.isSameFile(output.resolve("file"), output.resolve("new-link")));
        assertFalse(Files.isSameFile(output.resolve("old-link"), output.resolve("new-link")));
    }

    /** Backward aliases share an inode while staying below NTFS's limit of 1024 names per file. */
    @Test
    void longChainsUseOnePayloadAndDataMetadata() throws Exception {
        requireHardLinks();
        Path source = Files.createTempFile(temp, "many", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "link0", "data", null);
            for (int i = 1; i <= 512; i++) {
                entry(out, "link" + i, "", "link" + (i - 1));
            }
        }
        Path output = Files.createDirectory(temp.resolve("many-output"));
        TarUnArchiver unarchiver = unarchiver(source, output);
        unarchiver.extract();
        assertEquals("data", Files.readString(output.resolve("link0")));
        assertTrue(Files.isSameFile(output.resolve("link0"), output.resolve("link512")));
        assertEquals(
                1234567000000L,
                Files.getLastModifiedTime(output.resolve("link512")).toMillis());
    }

    /** Large logical chains exercise iterative content resolution without requiring thousands of NTFS links. */
    @Test
    void resolvesThousandsOfLinksWithoutFilesystemLinks() throws Exception {
        Path source = Files.createTempFile(temp, "logical-chain", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "link0", "data", null);
            for (int i = 1; i <= 2000; i++) {
                entry(out, "link" + i, "", "link" + (i - 1));
            }
        }
        try (TarFile file = new TarFile(source.toFile())) {
            Enumeration<ArchiveEntry> entries = file.getEntries();
            TarArchiveEntry last = null;
            int count = 0;
            while (entries.hasMoreElements()) {
                last = (TarArchiveEntry) entries.nextElement();
                count++;
            }
            assertEquals(2001, count);
            assertEquals("link2000", last.getName());
            assertEquals(0, last.getSize());
            TarResource resource = new TarResource(file, last);
            assertEquals(4, resource.getSize());
            try (var contents = resource.getContents()) {
                assertEquals("data", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /** Content-reading selectors use the configured decoder without disrupting streaming extraction. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(TarUnArchiver.UntarCompressionMethod.class)
    void compressedLinksAndContentSelectors(TarUnArchiver.UntarCompressionMethod method) throws Exception {
        requireHardLinks();
        Path source = archive(false);
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
            try (var out = new org.apache.commons.compress.compressors.CompressorStreamFactory()
                    .createCompressorOutputStream(algorithm, Files.newOutputStream(compressed))) {
                Files.copy(source, out);
            }
            source = compressed;
        }
        Path output = Files.createDirectory(temp.resolve("compressed-output"));
        TarUnArchiver unarchiver = unarchiver(source, output);
        unarchiver.setCompression(method);
        unarchiver.setFileSelectors(new FileSelector[] {
            file -> {
                if (!file.getName().equals("link")) {
                    return file.getName().equals("file");
                }
                try (var input = file.getContents()) {
                    return new String(input.readAllBytes(), StandardCharsets.UTF_8).equals("archive content");
                }
            }
        });
        unarchiver.extract();
        assertEquals("archive content", Files.readString(output.resolve("link")));
    }
    /** Applying a link header's timestamp or permissions would also modify the payload inode. */
    @Test
    void appliesOnlyDataBearingMetadata() throws Exception {
        requireHardLinks();
        Path source = Files.createTempFile(temp, "metadata", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "file", "content", null);
            TarArchiveEntry link = new TarArchiveEntry("link", TarConstants.LF_LINK);
            link.setLinkName("file");
            link.setMode(0600);
            link.setModTime(987654300000L);
            out.putArchiveEntry(link);
            out.closeArchiveEntry();
        }
        Path output = Files.createDirectory(temp.resolve("metadata-output"));
        unarchiver(source, output).extract();
        assertEquals(
                1234567000000L,
                Files.getLastModifiedTime(output.resolve("link")).toMillis());
        assertTrue(Files.isSameFile(output.resolve("file"), output.resolve("link")));
        if (Files.getFileStore(output).supportsFileAttributeView("posix")) {
            assertEquals(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"),
                    Files.getPosixFilePermissions(output.resolve("link")));
        }
    }

    /** Mapper collisions link to the current destination contents, matching GNU tar and bsdtar. */
    @Test
    void mappingCollisionsKeepArchiveOrder() throws Exception {
        requireHardLinks();
        Path source = Files.createTempFile(temp, "collision", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            entry(out, "file", "linked", null);
            entry(out, "other", "replacement", null);
            entry(out, "alias", "", "file");
        }
        Path output = Files.createDirectory(temp.resolve("collision-output"));
        TarUnArchiver unarchiver = unarchiver(source, output);
        java.util.concurrent.atomic.AtomicInteger mappings = new java.util.concurrent.atomic.AtomicInteger();
        unarchiver.setFileMappers(new FileMapper[] {
            name -> {
                mappings.incrementAndGet();
                return name.equals("alias") ? "alias" : "same";
            }
        });
        unarchiver.extract();
        assertEquals(3, mappings.get());
        assertEquals("replacement", Files.readString(output.resolve("same")));
        assertEquals("replacement", Files.readString(output.resolve("alias")));
        assertTrue(Files.isSameFile(output.resolve("same"), output.resolve("alias")));
    }
}
