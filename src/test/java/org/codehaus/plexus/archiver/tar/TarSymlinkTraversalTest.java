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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarSymlinkTraversalTest {
    @TempDir
    Path temp;

    /** Describes independently constructed headers; value is either regular contents or a link target. */
    private record Member(String name, char type, String value) {}

    /** Writes a fixture directly through the selected compressor, without using the production writer. */
    private Path archive(TarUnArchiver.UntarCompressionMethod compression, Member... members) throws Exception {
        Path archive = Files.createTempFile(temp, "symlinks", ".tar");
        String algorithm =
                switch (compression) {
                    case NONE -> null;
                    case GZIP -> "gz";
                    case BZIP2 -> "bzip2";
                    case SNAPPY -> "snappy-framed";
                    case XZ -> "xz";
                    case ZSTD -> "zstd";
                };
        try (OutputStream file = Files.newOutputStream(archive);
                OutputStream encoded = algorithm == null
                        ? file
                        : new CompressorStreamFactory().createCompressorOutputStream(algorithm, file);
                TarArchiveOutputStream out = new TarArchiveOutputStream(encoded)) {
            for (Member member : members) {
                TarArchiveEntry entry = new TarArchiveEntry(member.name, (byte) member.type);
                entry.setMode(0755);
                entry.setModTime(1234567000000L);
                byte[] bytes = member.value.getBytes(StandardCharsets.UTF_8);
                if (member.type == '0') {
                    entry.setSize(bytes.length);
                } else if (member.type == '1' || member.type == '2') {
                    entry.setLinkName(member.value);
                }
                out.putArchiveEntry(entry);
                if (member.type == '0') {
                    out.write(bytes);
                }
                out.closeArchiveEntry();
            }
        }
        return archive;
    }

    /** Creates a symlink only when the test filesystem permits it. */
    private void symlink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
    }

    /** Checks actual link capabilities, so unsupported filesystems skip only the affected cases. */
    private void requireLinks() throws IOException {
        Path original = Files.writeString(temp.resolve("probe"), "probe");
        symlink(temp.resolve("probe-symlink"), original);
        try {
            Files.createLink(temp.resolve("probe-hardlink"), original);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Hard links unavailable: " + e);
        }
    }

    /** Covers the inherited setting on each concrete compressed TAR extractor. */
    private TarUnArchiver extractor(Path archive, Path output, TarUnArchiver.UntarCompressionMethod compression) {
        TarUnArchiver extractor =
                switch (compression) {
                    case NONE -> new TarUnArchiver();
                    case GZIP -> new TarGZipUnArchiver();
                    case BZIP2 -> new TarBZip2UnArchiver();
                    case SNAPPY -> new TarSnappyUnArchiver();
                    case XZ -> new TarXZUnArchiver();
                    case ZSTD -> new TarZstdUnArchiver();
                };
        extractor.setSourceFile(archive.toFile());
        extractor.setDestDirectory(output.toFile());
        return extractor;
    }

    /** Exercises both policies through every TAR decoder. */
    static Stream<Arguments> policies() {
        return Arrays.stream(TarUnArchiver.UntarCompressionMethod.values())
                .flatMap(compression -> Stream.of(false, true).map(reject -> Arguments.of(compression, reject)));
    }

    /** GNU defaults use the current target; rejection stops before the redirected replacement is written. */
    @ParameterizedTest
    @MethodSource("policies")
    void directorySymlinkCollision(TarUnArchiver.UntarCompressionMethod compression, boolean reject) throws Exception {
        requireLinks();
        Path source = archive(
                compression,
                new Member("real/file", '0', "original"),
                new Member("redirect", '2', "real"),
                new Member("redirect/file", '0', "changed!"),
                new Member("alias", '1', "real/file"));
        Path output = Files.createDirectory(temp.resolve("output"));
        TarUnArchiver extractor = extractor(source, output, compression);
        assertFalse(extractor.isFailOnSymlinkTraversal());
        if (reject) {
            extractor.setFailOnSymlinkTraversal(true);
            assertTrue(extractor.isFailOnSymlinkTraversal());
            ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
            assertTrue(error.getMessage().contains("destination 'redirect/file'"));
            assertTrue(error.getMessage()
                    .contains("symbolic link '" + output.toRealPath().resolve("redirect") + "'"));
            assertEquals("original", Files.readString(output.resolve("real/file")));
            assertFalse(Files.exists(output.resolve("alias")));
        } else {
            extractor.extract();
            assertEquals("changed!", Files.readString(output.resolve("alias")));
            assertTrue(Files.isSameFile(output.resolve("alias"), output.resolve("real/file")));
        }
        assertTrue(Files.isSymbolicLink(output.resolve("redirect")));
    }

    /** Mapping and normalization must not conceal existing symlink parents, even with missing prefixes. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "redirect/file",
                "./redirect//file",
                "redirect/../file",
                "redirect/./file",
                "missing/../redirect/file",
                "real/../redirect/file",
                "redirect/new/file"
            })
    void checksMappedPathsBeforeNormalization(String mapped) throws Exception {
        Path output = Files.createDirectory(temp.resolve("output"));
        Files.createDirectory(output.resolve("real"));
        symlink(output.resolve("redirect"), Path.of("real"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("before", '0', "before"),
                new Member("logical", '0', "replacement"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(new FileMapper[] {name -> name.equals("logical") ? mapped : name});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains("entry 'logical'"));
        assertTrue(error.getMessage().contains("destination '" + mapped + "'"));
        assertTrue(error.getMessage().contains("symbolic link"));
        assertEquals("before", Files.readString(output.resolve("before")));
        try (var children = Files.list(output.resolve("real"))) {
            assertEquals(0, children.count());
        }
        assertFalse(Files.exists(output.resolve("file")));
        assertFalse(Files.exists(output.resolve("missing")));
    }

    /** Every output kind checks parents before creating directories, files, or additional links. */
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "5"})
    void rejectsAllEntryKindsBeforeChanges(String kind) throws Exception {
        requireLinks();
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("real/file", '0', "original"),
                new Member("redirect", '2', "real"),
                new Member("redirect/new/entry", kind.charAt(0), "real/file"));
        Path output = Files.createDirectory(temp.resolve("output"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains("destination 'redirect/new/entry"));
        assertFalse(Files.exists(output.resolve("real/new")));
        assertEquals("original", Files.readString(output.resolve("real/file")));
    }

    /** Excluded source members still require checking their mapped path when a selected hard link uses it. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void checksMappedHardLinkTarget(boolean reject) throws Exception {
        requireLinks();
        Path output = Files.createDirectory(temp.resolve("output"));
        Files.createDirectory(output.resolve("real"));
        Files.writeString(output.resolve("real/file"), "existing");
        symlink(output.resolve("redirect"), Path.of("real"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("source", '0', "archive"),
                new Member("alias", '1', "source"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("source")});
        extractor.setFileMappers(new FileMapper[] {name -> name.equals("source") ? "redirect/file" : name});
        extractor.setFailOnSymlinkTraversal(reject);
        if (reject) {
            ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
            assertTrue(error.getMessage().contains("entry 'alias'"));
            assertTrue(error.getMessage().contains("hard-link target 'redirect/file'"));
            assertFalse(Files.exists(output.resolve("alias")));
        } else {
            extractor.extract();
            assertEquals("existing", Files.readString(output.resolve("alias")));
            assertTrue(Files.isSameFile(output.resolve("alias"), output.resolve("real/file")));
        }
    }

    /** Selection skips unsafe entries, but overwrite policy does not bypass validation of selected paths. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void selectionAndOverwrite(boolean excluded) throws Exception {
        Path output = Files.createDirectory(temp.resolve("output"));
        Files.createDirectory(output.resolve("real"));
        Files.writeString(output.resolve("real/file"), "existing");
        symlink(output.resolve("redirect"), Path.of("real"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("redirect/file", '0', "replacement"),
                new Member("after", '0', "after"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setOverwrite(false);
        if (excluded) {
            extractor.setFileSelectors(
                    new FileSelector[] {file -> !file.getName().equals("redirect/file")});
            extractor.extract();
            assertEquals("after", Files.readString(output.resolve("after")));
        } else {
            assertThrows(ArchiverException.class, extractor::extract);
            assertFalse(Files.exists(output.resolve("after")));
        }
        assertEquals("existing", Files.readString(output.resolve("real/file")));
    }

    /** The configured root is trusted, while unrelated symbolic-link entries remain allowed. */
    @Test
    void acceptsTrustedRootAndUnrelatedSymlinks() throws Exception {
        Path realRoot = Files.createDirectory(temp.resolve("real-root"));
        Path output = temp.resolve("output");
        symlink(output, realRoot);
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("redirect", '2', "elsewhere"),
                new Member("./nested/../directory/file", '0', "content"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.extract();
        assertEquals("content", Files.readString(realRoot.resolve("directory/file")));
        assertTrue(Files.isSymbolicLink(realRoot.resolve("redirect")));
    }

    /** Builds a trusted root whose lexical parent differs from its filesystem-resolved parent. */
    private Path rootThroughSymlinkAndParent() throws IOException {
        Path actual = Files.createDirectory(temp.resolve("actual"));
        Files.createDirectory(actual.resolve("sub"));
        Path lexical = Files.createDirectory(temp.resolve("lexical"));
        symlink(lexical.resolve("hop"), actual.resolve("sub"));
        return lexical.resolve("hop/..");
    }

    /** Both relative and absolute mappings must inspect the actual children of a trusted root alias. */
    @ParameterizedTest
    @ValueSource(strings = {"relative", "configured-absolute", "canonical-absolute"})
    void checksChildrenOfRootThroughSymlinkAndParent(String spelling) throws Exception {
        Path output = rootThroughSymlinkAndParent();
        Path actual = output.toFile().getCanonicalFile().toPath();
        Files.createDirectory(actual.resolve("real"));
        symlink(actual.resolve("redirect"), Path.of("real"));
        String mapped =
                switch (spelling) {
                    case "configured-absolute" ->
                        output.resolve("redirect/file").toString();
                    case "canonical-absolute" -> actual.resolve("redirect/file").toString();
                    default -> "redirect/file";
                };
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("logical", '0', "changed"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(new FileMapper[] {name -> mapped});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains("entry 'logical'"));
        assertTrue(error.getMessage().contains("symbolic link '" + actual.resolve("redirect") + "'"));
        assertFalse(Files.exists(actual.resolve("real/file")));
    }

    /** A selected link checks its current destination and target beneath the actual trusted root. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void checksHardLinksUnderRootThroughSymlinkAndParent(boolean targetTraversal) throws Exception {
        requireLinks();
        Path output = rootThroughSymlinkAndParent();
        Path actual = output.toFile().getCanonicalFile().toPath();
        Files.createDirectory(actual.resolve("real"));
        Files.writeString(actual.resolve("real/file"), "existing");
        symlink(actual.resolve("redirect"), Path.of("real"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("source", '0', "archive"),
                new Member("alias", '1', "source"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("source")});
        extractor.setFileMappers(new FileMapper[] {
            name -> name.equals("source")
                    ? (targetTraversal ? "redirect/file" : "real/file")
                    : (targetTraversal ? "alias" : "redirect/alias")
        });
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains(targetTraversal ? "hard-link target" : "destination"));
        assertTrue(error.getMessage().contains("symbolic link '" + actual.resolve("redirect") + "'"));
        assertFalse(Files.exists(actual.resolve("alias")));
        assertFalse(Files.exists(actual.resolve("real/alias")));
        assertEquals("existing", Files.readString(actual.resolve("real/file")));
    }

    /** Unrelated symlinks in the lexical tree must not reject safe extraction in the actual tree. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsSafeChildrenOfRootThroughSymlinkAndParent(boolean reject) throws Exception {
        requireLinks();
        Path output = rootThroughSymlinkAndParent();
        Path actual = output.toFile().getCanonicalFile().toPath();
        symlink(temp.resolve("lexical/real"), Path.of("elsewhere"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("real/file", '0', "content"),
                new Member("alias", '1', "real/file"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(reject);
        extractor.extract();
        assertEquals("content", Files.readString(actual.resolve("alias")));
        assertTrue(Files.isSameFile(actual.resolve("real/file"), actual.resolve("alias")));
        assertTrue(Files.isSymbolicLink(temp.resolve("lexical/real")));
    }

    /** Dot components in either root spelling must not prevent safe regular-file and hard-link extraction. */
    @ParameterizedTest
    @CsvSource({
        "output, output",
        "output, ./output",
        "./output, output",
        "output/., output",
        "./output/., ./output/.",
        "output, actual",
        "lexical/./hop/.., lexical/hop/../.",
        "lexical/hop/../., lexical/./hop/.."
    })
    void acceptsHarmlessRootSpellings(String configured, String mapped) throws Exception {
        requireLinks();
        Path actual = Files.createDirectory(temp.resolve("actual"));
        Files.createDirectory(actual.resolve("sub"));
        symlink(temp.resolve("output"), actual);
        Files.createDirectory(temp.resolve("lexical"));
        symlink(temp.resolve("lexical/hop"), actual.resolve("sub"));
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("file", '0', "content"),
                new Member("alias", '1', "file"));
        TarUnArchiver extractor =
                extractor(source, temp.resolve(configured), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> temp.resolve(mapped).resolve(name).toString()});
        extractor.extract();
        assertEquals("content", Files.readString(actual.resolve("file")));
        assertTrue(Files.isSameFile(actual.resolve("file"), actual.resolve("alias")));
    }

    /** Matching a dotted root must leave its suffix intact, including terminal dots and symlinks before parents. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "./redirect/file",
                "redirect/../file",
                "redirect/./../file",
                "redirect/.",
                "missing/../redirect/file"
            })
    void dottedRootStillChecksEntrySuffix(String suffix) throws Exception {
        Path actual = Files.createDirectory(temp.resolve("actual"));
        Files.createDirectory(actual.resolve("real"));
        symlink(actual.resolve("redirect"), Path.of("real"));
        symlink(temp.resolve("output"), actual);
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("logical", '0', "changed"));
        TarUnArchiver extractor =
                extractor(source, temp.resolve("./output/."), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> temp.resolve("output").resolve(suffix).toString()});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        // Rejection must identify the symlink beneath the trusted root, not the root alias itself.
        assertTrue(error.getMessage()
                .contains("symbolic link '" + actual.toRealPath().resolve("redirect") + "'"));
        assertFalse(Files.exists(actual.resolve("file")));
        assertFalse(Files.exists(actual.resolve("real/file")));
    }

    /** Both hard-link paths retain intermediate symlink checks after ignoring dots in the trusted prefix. */
    @ParameterizedTest
    @CsvSource({"false, redirect/file", "true, redirect/file", "false, redirect/../file", "true, redirect/../file"})
    void dottedRootStillChecksHardLinkPaths(boolean targetTraversal, String suffix) throws Exception {
        requireLinks();
        Path actual = Files.createDirectory(temp.resolve("actual"));
        Files.createDirectory(actual.resolve("real"));
        Files.writeString(actual.resolve("real/file"), "existing");
        symlink(actual.resolve("redirect"), Path.of("real"));
        symlink(temp.resolve("output"), actual);
        String unsafe = temp.resolve("output").resolve(suffix).toString();
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("source", '0', "archive"),
                new Member("alias", '1', "source"));
        TarUnArchiver extractor =
                extractor(source, temp.resolve("./output/."), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("source")});
        extractor.setFileMappers(new FileMapper[] {
            name -> name.equals("source")
                    ? (targetTraversal ? unsafe : "real/file")
                    : (targetTraversal ? "alias" : unsafe)
        });
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains(targetTraversal ? "hard-link target" : "destination"));
        assertTrue(error.getMessage()
                .contains("symbolic link '" + actual.toRealPath().resolve("redirect") + "'"));
        assertEquals("existing", Files.readString(actual.resolve("real/file")));
        assertFalse(Files.exists(actual.resolve("alias")));
        assertFalse(Files.exists(actual.resolve("file")));
    }

    /** Models a temporary directory reached through an ancestor symlink, such as /var on macOS. */
    private Path rootUnderSymlinkedParent() throws IOException {
        Path parent = Files.createDirectory(temp.resolve("parent"));
        Files.createDirectory(parent.resolve("actual"));
        symlink(temp.resolve("parent-alias"), parent);
        symlink(parent.resolve("output"), Path.of("actual"));
        return temp.resolve("parent-alias/actual");
    }

    /** Absolute mappings may name the actual root through an ancestor alias rather than the configured root alias. */
    @ParameterizedTest
    @EnumSource(TarUnArchiver.UntarCompressionMethod.class)
    void acceptsRootUnderSymlinkedParent(TarUnArchiver.UntarCompressionMethod compression) throws Exception {
        requireLinks();
        Path mappedRoot = rootUnderSymlinkedParent();
        Path actual = mappedRoot.toRealPath();
        Path source = archive(compression, new Member("file", '0', "content"), new Member("alias", '1', "file"));
        TarUnArchiver extractor = extractor(source, mappedRoot.resolveSibling("output"), compression);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> mappedRoot.resolve(name).toString()});
        extractor.extract();
        assertEquals("content", Files.readString(actual.resolve("file")));
        assertTrue(Files.isSameFile(actual.resolve("file"), actual.resolve("alias")));
    }

    /** Resolving an ancestor alias must not conceal symlinks in the remaining mapped path. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "./redirect/file",
                "redirect/../file",
                "redirect/./../file",
                "redirect/.",
                "missing/../redirect/file"
            })
    void checksEntrySuffixUnderSymlinkedParent(String suffix) throws Exception {
        Path mappedRoot = rootUnderSymlinkedParent();
        Path actual = mappedRoot.toRealPath();
        Files.createDirectory(actual.resolve("real"));
        Files.writeString(actual.resolve("real/file"), "existing");
        Files.writeString(actual.resolve("file"), "existing");
        symlink(actual.resolve("redirect"), Path.of("real"));
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("logical", '0', "changed"));
        TarUnArchiver extractor =
                extractor(source, mappedRoot.resolveSibling("output"), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> mappedRoot.resolve(suffix).toString()});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(
                error.getMessage().contains("symbolic link '" + actual.resolve("redirect") + "'"), error::getMessage);
        assertEquals("existing", Files.readString(actual.resolve("file")));
        assertEquals("existing", Files.readString(actual.resolve("real/file")));
        assertTrue(Files.isSymbolicLink(actual.resolve("redirect")));
        assertFalse(Files.exists(actual.resolve("missing")));
    }

    /** Both hard-link paths must retain suffix checks after an ancestor alias is resolved. */
    @ParameterizedTest
    @CsvSource({"false, redirect/alias", "true, redirect/file", "false, redirect/../alias", "true, redirect/../file"})
    void checksHardLinkPathsUnderSymlinkedParent(boolean targetTraversal, String suffix) throws Exception {
        requireLinks();
        Path mappedRoot = rootUnderSymlinkedParent();
        Path actual = mappedRoot.toRealPath();
        Files.createDirectory(actual.resolve("real"));
        Files.writeString(actual.resolve("real/file"), "existing");
        Files.writeString(actual.resolve("file"), "existing");
        symlink(actual.resolve("redirect"), Path.of("real"));
        String unsafe = mappedRoot.resolve(suffix).toString();
        Path source = archive(
                TarUnArchiver.UntarCompressionMethod.NONE,
                new Member("source", '0', "archive"),
                new Member("alias", '1', "source"));
        TarUnArchiver extractor =
                extractor(source, mappedRoot.resolveSibling("output"), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("source")});
        extractor.setFileMappers(new FileMapper[] {
            name -> name.equals("source")
                    ? (targetTraversal ? unsafe : "real/file")
                    : (targetTraversal ? "alias" : unsafe)
        });
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains(targetTraversal ? "hard-link target" : "destination"));
        assertTrue(
                error.getMessage().contains("symbolic link '" + actual.resolve("redirect") + "'"), error::getMessage);
        assertEquals("existing", Files.readString(actual.resolve("file")));
        assertEquals("existing", Files.readString(actual.resolve("real/file")));
        assertFalse(Files.exists(actual.resolve("alias")));
        assertFalse(Files.exists(actual.resolve("real/alias")));
    }

    /** An ancestor alias is trusted, but a separate alias pointing directly at the root is not. */
    @Test
    void rejectsUnconfiguredRootAlias() throws Exception {
        Path mappedRoot = rootUnderSymlinkedParent();
        Path actual = mappedRoot.toRealPath();
        Path alternate = mappedRoot.resolveSibling("alternate");
        symlink(alternate, Path.of("actual"));
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("file", '0', "content"));
        TarUnArchiver extractor =
                extractor(source, mappedRoot.resolveSibling("output"), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> alternate.resolve(name).toString()});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(
                error.getMessage().contains("symbolic link '" + actual.resolveSibling("alternate") + "'"),
                error::getMessage);
        assertFalse(Files.exists(actual.resolve("file")));
    }

    /** Reaching the root ends ancestor exemptions, even if a later parent component leaves the root again. */
    @ParameterizedTest
    @ValueSource(strings = {"back/actual/file", "../back/actual/file"})
    void rejectsAncestorAliasAfterReachingRoot(String suffix) throws Exception {
        Path mappedRoot = rootUnderSymlinkedParent();
        Path actual = mappedRoot.toRealPath();
        Files.writeString(actual.resolve("file"), "existing");
        Path link = (suffix.startsWith("../") ? actual.getParent() : actual).resolve("back");
        symlink(link, actual.getParent());
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("file", '0', "changed"));
        TarUnArchiver extractor =
                extractor(source, mappedRoot.resolveSibling("output"), TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(
                new FileMapper[] {name -> mappedRoot.resolve(suffix).toString()});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains("symbolic link '" + link + "'"), error::getMessage);
        assertEquals("existing", Files.readString(actual.resolve("file")));
    }

    /** Absolute mappings must be checked before FileUtils canonicalization erases the symlink components. */
    @ParameterizedTest
    @ValueSource(strings = {"lexical", "canonical", "alternate", "backslash"})
    void checksAbsoluteMappedPaths(String spelling) throws Exception {
        Path realRoot = Files.createDirectory(temp.resolve("real-root"));
        Files.createDirectory(realRoot.resolve("real"));
        symlink(realRoot.resolve("redirect"), Path.of("real"));
        Path output = temp.resolve("output");
        symlink(output, realRoot);
        Path alternate = temp.resolve("alternate");
        symlink(alternate, realRoot);
        String mapped =
                switch (spelling) {
                    case "canonical" -> realRoot.resolve("redirect/file").toString();
                    case "alternate" -> alternate.resolve("redirect/file").toString();
                    case "backslash" -> output.resolve("redirect").toString() + "\\file";
                    default -> output.resolve("redirect/file").toString();
                };
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("logical", '0', "content"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.setFileMappers(new FileMapper[] {name -> mapped});
        ArchiverException error = assertThrows(ArchiverException.class, extractor::extract);
        assertTrue(error.getMessage().contains("destination '" + mapped + "'"));
        assertTrue(error.getMessage().contains("symbolic link"));
        assertFalse(Files.exists(realRoot.resolve("real/file")));
    }

    /** Relative backslashes remain literal on Unix, matching the existing extraction path resolver. */
    @Test
    void acceptsLiteralRelativeBackslashes() throws Exception {
        assumeTrue(java.io.File.separatorChar == '/', "Backslash is a path separator on this platform");
        Path output = Files.createDirectory(temp.resolve("output"));
        symlink(output.resolve("redirect"), Path.of("elsewhere"));
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("redirect\\file", '0', "content"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        extractor.extract();
        assertEquals("content", Files.readString(output.resolve("redirect\\file")));
    }

    /** Switching the instance policy after a failure takes effect without retaining per-archive path state. */
    @Test
    void canDisableRejectionAfterFailure() throws Exception {
        Path output = Files.createDirectory(temp.resolve("output"));
        Files.createDirectory(output.resolve("real"));
        symlink(output.resolve("redirect"), Path.of("real"));
        Path source = archive(TarUnArchiver.UntarCompressionMethod.NONE, new Member("redirect/file", '0', "content"));
        TarUnArchiver extractor = extractor(source, output, TarUnArchiver.UntarCompressionMethod.NONE);
        extractor.setFailOnSymlinkTraversal(true);
        assertThrows(ArchiverException.class, extractor::extract);
        extractor.setFailOnSymlinkTraversal(false);
        extractor.extract();
        assertEquals("content", Files.readString(output.resolve("real/file")));
    }
}
