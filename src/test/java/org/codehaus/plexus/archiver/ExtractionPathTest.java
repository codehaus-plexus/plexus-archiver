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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exercises the shared path policy through both archive readers and the protected extraction hook. */
class ExtractionPathTest {
    enum Format {
        SHARED,
        TAR,
        ZIP
    }

    enum Kind {
        FILE,
        DIRECTORY,
        SYMLINK
    }

    @TempDir
    Path temp;

    private static Stream<Arguments> ordinaryEntries() {
        return Arrays.stream(Format.values())
                .flatMap(format -> Stream.of(Kind.FILE, Kind.DIRECTORY).map(kind -> Arguments.of(format, kind)));
    }

    private static Stream<Arguments> finalLinks() {
        return ordinaryEntries().flatMap(arguments -> Stream.of("inside", "outside", "missing")
                .map(target -> Arguments.of(arguments.get()[0], arguments.get()[1], target)));
    }

    private static Stream<Arguments> windowsRootedMappings() {
        return Arrays.stream(Format.values()).flatMap(format -> Stream.of("rooted", "rooted-forward", "drive-relative")
                .flatMap(spelling ->
                        Stream.of(false, true).map(contained -> Arguments.of(format, spelling, contained))));
    }

    private void symlink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
    }

    /** Uses an independent archive writer and a mapper so absolute names reach the extractor unchanged. */
    private void extract(Format format, Path root, String name, Kind kind, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        String member = kind == Kind.DIRECTORY ? "member/" : "member";
        Path archive = Files.createTempFile(temp, "path", ".archive");
        AbstractUnArchiver extractor;
        if (format == Format.TAR) {
            try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
                byte type = (byte) (kind == Kind.FILE ? '0' : kind == Kind.DIRECTORY ? '5' : '2');
                TarArchiveEntry entry = new TarArchiveEntry(member, type);
                entry.setModTime(1234567000000L);
                entry.setMode(0755);
                if (kind == Kind.FILE) {
                    entry.setSize(bytes.length);
                } else if (kind == Kind.SYMLINK) {
                    entry.setLinkName(value);
                }
                out.putArchiveEntry(entry);
                if (kind == Kind.FILE) {
                    out.write(bytes);
                }
                out.closeArchiveEntry();
            }
            extractor = new TarUnArchiver();
        } else if (format == Format.ZIP) {
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
                ZipArchiveEntry entry = new ZipArchiveEntry(member);
                entry.setUnixMode((kind == Kind.SYMLINK ? 0120000 : kind == Kind.DIRECTORY ? 0040000 : 0100000) | 0755);
                entry.setTime(1234567000000L);
                out.putArchiveEntry(entry);
                if (kind != Kind.DIRECTORY) {
                    out.write(bytes);
                }
                out.closeArchiveEntry();
            }
            extractor = new ZipUnArchiver();
        } else {
            extractor = new AbstractUnArchiver() {
                @Override
                protected void execute() {
                    try {
                        extractFile(
                                getSourceFile(),
                                getDestDirectory(),
                                new ByteArrayInputStream(bytes),
                                member,
                                new Date(1234567000000L),
                                kind == Kind.DIRECTORY,
                                0755,
                                kind == Kind.SYMLINK ? value : null,
                                getFileMappers());
                    } catch (IOException e) {
                        throw new ArchiverException("Cannot extract fixture", e);
                    }
                }

                @Override
                protected void execute(String path, File outputDirectory) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        extractor.setSourceFile(archive.toFile());
        extractor.setDestDirectory(root.toFile());
        extractor.setFileMappers(new FileMapper[] {ignored -> name});
        extractor.extract();
    }

    /** A missing leaf must not hide the physical location of an existing symlinked parent on Windows. */
    @ParameterizedTest
    @MethodSource("ordinaryEntries")
    void rejectsOutsideParentBeforeCreatingDirectories(Format format, Kind kind) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        symlink(root.resolve("redirect"), outside);
        assertThrows(ArchiverException.class, () -> extract(format, root, "redirect/new/entry", kind, "replacement"));
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
        assertTrue(Files.isSymbolicLink(root.resolve("redirect")));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void rejectsOutsideParentWithoutChangingExistingFile(Format format) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Path file = Files.writeString(outside.resolve("file"), "original");
        symlink(root.resolve("redirect"), outside);
        assertThrows(ArchiverException.class, () -> extract(format, root, "redirect/file", Kind.FILE, "replacement"));
        assertEquals("original", Files.readString(file));
    }

    /** Windows rooted paths can be non-absolute and must retain Path.resolve semantics through mappers. */
    @ParameterizedTest
    @MethodSource("windowsRootedMappings")
    void preservesWindowsRootedMappings(Format format, String spelling, boolean contained) throws Exception {
        assumeTrue(File.separatorChar == '\\', "Requires Windows path syntax");
        Path root = Files.createDirectory(temp.resolve("root")).toAbsolutePath();
        Path outside = Files.createDirectory(temp.resolve("outside")).toAbsolutePath();
        Path expected = (contained ? root : outside).resolve("file.txt");
        String mapped;
        if (spelling.equals("drive-relative")) {
            String drive = root.getRoot().toString();
            assumeTrue(drive.length() == 3 && drive.charAt(1) == ':', "Requires a drive-letter destination");
            mapped = drive.substring(0, 2) + (contained ? "file.txt" : "..\\outside\\file.txt");
        } else {
            mapped = "\\" + root.getRoot().relativize(expected);
            if (spelling.equals("rooted-forward")) {
                mapped = mapped.replace('\\', '/');
            }
        }
        Path path = Path.of(mapped);
        assertFalse(path.isAbsolute());
        assertNotNull(path.getRoot());
        String name = mapped;
        if (contained) {
            extract(format, root, name, Kind.FILE, "content");
            assertEquals("content", Files.readString(expected));
            try (var files = Files.list(root)) {
                assertEquals(1, files.count());
            }
        } else {
            assertThrows(ArchiverException.class, () -> extract(format, root, name, Kind.FILE, "content"));
            assertFalse(Files.exists(expected));
            try (var files = Files.list(root)) {
                assertEquals(0, files.count());
            }
        }
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    /** Both a safe root alias and a contained directory link must work with nonexistent descendants. */
    @ParameterizedTest
    @EnumSource(Format.class)
    void acceptsAliasesWithMissingDescendants(Format format) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path real = Files.createDirectory(root.resolve("real"));
        symlink(root.resolve("redirect"), Path.of("real"));
        Path alias = temp.resolve("alias");
        symlink(alias, root);
        extract(format, alias, "redirect/new/file", Kind.FILE, "relative");
        extract(format, alias, alias.resolve("redirect/other/file").toString(), Kind.FILE, "absolute");
        assertEquals("relative", Files.readString(real.resolve("new/file")));
        assertEquals("absolute", Files.readString(real.resolve("other/file")));
    }

    /** Validation and writes must agree on link/.. even when the lexical destination does not exist. */
    @ParameterizedTest
    @EnumSource(Format.class)
    void resolvesRootLinkBeforeParentComponent(Format format) throws Exception {
        Path lexical = Files.createDirectory(temp.resolve("lexical"));
        Path actual = Files.createDirectory(temp.resolve("actual"));
        Path sub = Files.createDirectory(actual.resolve("sub"));
        Path destination = Files.createDirectory(actual.resolve("only"));
        symlink(lexical.resolve("hop"), sub);
        Path root = lexical.resolve("hop/../only");
        extract(format, root, "file", Kind.FILE, "content");
        extract(format, root, root.resolve("absolute").toString(), Kind.FILE, "mapped");
        assertEquals("content", Files.readString(destination.resolve("file")));
        assertEquals("mapped", Files.readString(destination.resolve("absolute")));
        assertFalse(Files.exists(lexical.resolve("only")));
    }

    /** Ordinary entries must preserve final links and their targets, including dangling links. */
    @ParameterizedTest
    @MethodSource("finalLinks")
    void rejectsFinalSymlink(Format format, Kind kind, String location) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path target = (location.equals("inside") ? root : temp).resolve("target");
        if (!location.equals("missing")) {
            if (kind == Kind.DIRECTORY) {
                Files.createDirectory(target);
            } else {
                Files.writeString(target, "original");
            }
            Files.setLastModifiedTime(target, FileTime.fromMillis(1500000000000L));
        }
        Path link = root.resolve("entry");
        symlink(link, target);
        assertThrows(ArchiverException.class, () -> extract(format, root, "entry", kind, "replacement"));
        assertEquals(target, Files.readSymbolicLink(link));
        if (location.equals("missing")) {
            assertFalse(Files.exists(target, LinkOption.NOFOLLOW_LINKS));
        } else if (kind == Kind.FILE) {
            assertEquals("original", Files.readString(target));
        } else {
            assertTrue(Files.isDirectory(target));
        }
        if (!location.equals("missing")) {
            assertEquals(1500000000000L, Files.getLastModifiedTime(target).toMillis());
        }
    }

    /** A symlink member may name an outside target, but must not set metadata on that target. */
    @ParameterizedTest
    @EnumSource(Format.class)
    void symlinkMetadataDoesNotFollowTarget(Format format) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path target = Files.writeString(temp.resolve("target"), "original");
        symlink(temp.resolve("probe"), target);
        Files.setLastModifiedTime(target, FileTime.fromMillis(1500000000000L));
        var attributes = Files.readAttributes(target, "basic:lastModifiedTime");
        var permissions = Files.getFileStore(target).supportsFileAttributeView("posix")
                ? Files.getPosixFilePermissions(target)
                : null;
        extract(format, root, "entry", Kind.SYMLINK, target.toString());
        assertEquals(attributes, Files.readAttributes(target, "basic:lastModifiedTime"));
        // Repeated extraction also preserves an already existing symlink and its target's metadata.
        extract(format, root, "entry", Kind.SYMLINK, target.toString());
        assertEquals(target, Files.readSymbolicLink(root.resolve("entry")));
        assertEquals(attributes, Files.readAttributes(target, "basic:lastModifiedTime"));
        if (permissions != null) {
            assertEquals(permissions, Files.getPosixFilePermissions(target));
        }
        assertEquals("original", Files.readString(target));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @Timeout(5)
    void rejectsSymlinkCycles(Format format) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        symlink(root.resolve("loop"), Path.of("loop"));
        Throwable error =
                assertThrows(ArchiverException.class, () -> extract(format, root, "loop/file", Kind.FILE, "content"));
        while (error.getCause() != null) {
            error = error.getCause();
        }
        assertInstanceOf(FileSystemLoopException.class, error);
    }

    /** Resolve long chains ourselves instead of depending on an operating system's whole-path link limit. */
    @ParameterizedTest
    @EnumSource(Format.class)
    void boundsSymlinkExpansions(Format format) throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path real = Files.createDirectory(root.resolve("real"));
        for (int i = 64; i >= 0; i--) {
            symlink(root.resolve("link" + i), Path.of(i == 64 ? "real" : "link" + (i + 1)));
        }
        extract(format, root, "link1/accepted", Kind.FILE, "content");
        assertEquals("content", Files.readString(real.resolve("accepted")));
        Throwable error = assertThrows(
                ArchiverException.class, () -> extract(format, root, "link0/rejected", Kind.FILE, "content"));
        while (error.getCause() != null) {
            error = error.getCause();
        }
        assertInstanceOf(FileSystemLoopException.class, error);
        assertFalse(Files.exists(real.resolve("rejected")));
    }
}
