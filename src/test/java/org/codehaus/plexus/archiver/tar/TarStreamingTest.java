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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TarStreamingTest {
    @TempDir
    Path temp;

    /** Exercises the streaming contract both with and without links through every decoder. */
    static Stream<Arguments> extractionCases() {
        return Arrays.stream(TarUnArchiver.UntarCompressionMethod.values())
                .flatMap(method -> Stream.of(false, true).flatMap(links -> Stream.of(false, true)
                        .map(rejectTraversal -> Arguments.of(method, links, rejectTraversal))));
    }

    /** Subclass hooks see original arguments while stateful mapping is shared by checks, output and link targets. */
    @ParameterizedTest
    @MethodSource("extractionCases")
    void preservesMappedExtractionHookArguments(
            TarUnArchiver.UntarCompressionMethod method, boolean links, boolean rejectTraversal) throws Exception {
        Path source = temp.resolve("hook.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "payload", null);
            TarHardLinkTest.entry(out, "ordinary", "skip this", null);
            if (links) {
                TarHardLinkTest.entry(out, "alias", "", "data");
            }
        }
        Path output = Files.createDirectory(temp.resolve("hook-output"));
        AtomicInteger mappings = new AtomicInteger();
        FileMapper[] mappers = {name -> "mapped/" + name + "-" + mappings.incrementAndGet(), name -> "prefix/" + name};
        List<String> names = new ArrayList<>();
        List<FileMapper[]> mapperArguments = new ArrayList<>();
        TarUnArchiver extractor = new TarUnArchiver(compress(source, method).toFile()) {
            /** Models an existing subclass that selects by source name and delegates with the supplied mappers. */
            @Override
            protected void extractFile(
                    File archive,
                    File directory,
                    InputStream contents,
                    String name,
                    Date date,
                    boolean isDirectory,
                    Integer mode,
                    String symlink,
                    FileMapper[] fileMappers)
                    throws IOException {
                names.add(name);
                mapperArguments.add(fileMappers);
                if (!name.equals("ordinary")) {
                    super.extractFile(
                            archive, directory, contents, name, date, isDirectory, mode, symlink, fileMappers);
                }
            }
        };
        extractor.setCompression(method);
        extractor.setDestDirectory(output.toFile());
        extractor.setFileMappers(mappers);
        extractor.setFailOnSymlinkTraversal(rejectTraversal);
        extractor.extract();

        assertEquals(List.of("data", "ordinary"), names);
        mapperArguments.forEach(argument -> assertSame(mappers, argument));
        assertEquals(links ? 3 : 2, mappings.get(), "each selected occurrence must be mapped only once");
        assertEquals("payload", Files.readString(output.resolve("prefix/mapped/data-1")));
        assertFalse(Files.exists(output.resolve("prefix/mapped/ordinary-2")));
        if (links) {
            assertEquals("payload", Files.readString(output.resolve("prefix/mapped/alias-3")));
            assertTrue(
                    Files.isSameFile(output.resolve("prefix/mapped/data-1"), output.resolve("prefix/mapped/alias-3")));
        }
    }

    /** A failed subclass hook must release its mapping before a later direct call to the protected method. */
    @Test
    void clearsMappedHookStateAfterFailure() throws Exception {
        Path source = temp.resolve("failed-hook.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "ordinary", "payload", null);
        }
        Path output = Files.createDirectory(temp.resolve("failed-hook-output"));
        AtomicInteger mappings = new AtomicInteger();
        FileMapper[] mappers = {name -> name + "-" + mappings.incrementAndGet()};
        class FailingExtractor extends TarUnArchiver {
            /** Models a subclass that aborts extraction after the destination was mapped. */
            @Override
            protected void extractFile(
                    File archive,
                    File directory,
                    InputStream contents,
                    String name,
                    Date date,
                    boolean isDirectory,
                    Integer mode,
                    String symlink,
                    FileMapper[] fileMappers)
                    throws IOException {
                throw new IOException("hook failure");
            }

            /** Exercises a super call outside the archive loop using the same original name and mapper array. */
            void extractDirectly() throws IOException {
                try (InputStream contents = new ByteArrayInputStream("direct".getBytes(StandardCharsets.UTF_8))) {
                    super.extractFile(
                            source.toFile(),
                            output.toFile(),
                            contents,
                            "ordinary",
                            new Date(),
                            false,
                            0644,
                            null,
                            mappers);
                }
            }
        }
        FailingExtractor extractor = new FailingExtractor();
        extractor.setSourceFile(source.toFile());
        extractor.setDestDirectory(output.toFile());
        extractor.setFileMappers(mappers);
        assertThrows(ArchiverException.class, extractor::extract);
        extractor.extractDirectly();
        assertEquals(2, mappings.get());
        assertFalse(Files.exists(output.resolve("ordinary-1")));
        assertEquals("direct", Files.readString(output.resolve("ordinary-2")));
    }

    /** A large later member detects an up-front scan even when buffered input reads ahead. */
    @ParameterizedTest
    @MethodSource("extractionCases")
    void extractsBeforeReadingLaterPayload(
            TarUnArchiver.UntarCompressionMethod method, boolean links, boolean rejectTraversal) throws Exception {
        Path source = temp.resolve("source.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "early", "first", null);
            TarHardLinkTest.entry(out, "bulk", "x".repeat(128 * 1024), null);
            if (links) {
                TarHardLinkTest.entry(out, "alias", "", "early");
            }
        }
        long sourceSize = Files.size(source);
        Path encoded = compress(source, method);
        Path output = Files.createDirectory(temp.resolve("output"));
        AtomicInteger opens = new AtomicInteger();
        AtomicLong bytes = new AtomicLong();
        TarUnArchiver extractor = new TarUnArchiver(encoded.toFile()) {
            /** Observes decompressed reads while retaining the production compression configuration. */
            @Override
            protected TarFile newTarFile(File file) {
                TarFile decoder = super.newTarFile(file);
                return new TarFile(file) {
                    /** Counts source opens and verifies output exists before later data is consumed. */
                    @Override
                    protected InputStream getInputStream(File source) throws IOException {
                        opens.incrementAndGet();
                        return new FilterInputStream(decoder.getInputStream(source)) {
                            /** Counts single-byte reads as well as the usual block reads. */
                            @Override
                            public int read() throws IOException {
                                int value = in.read();
                                observe(value < 0 ? 0 : 1);
                                return value;
                            }

                            /** Measures decompressed bytes, so compression ratio cannot hide a pre-scan. */
                            @Override
                            public int read(byte[] buffer, int offset, int length) throws IOException {
                                int count = in.read(buffer, offset, length);
                                observe(Math.max(count, 0));
                                return count;
                            }

                            /** Header-only scanning must not escape detection through skip operations. */
                            @Override
                            public long skip(long count) throws IOException {
                                long skipped = in.skip(count);
                                observe(skipped);
                                return skipped;
                            }

                            /** Allows buffer read-ahead while rejecting a full preliminary traversal. */
                            private void observe(long count) throws IOException {
                                if (bytes.addAndGet(count) > 64 * 1024) {
                                    assertEquals("first", Files.readString(output.resolve("early")));
                                }
                            }
                        };
                    }
                };
            }
        };
        extractor.setCompression(method);
        extractor.setFailOnSymlinkTraversal(rejectTraversal);
        extractor.setDestDirectory(output.toFile());
        extractor.extract();
        assertEquals(1, opens.get(), "ordinary extraction must never open a replay cursor");
        assertTrue(bytes.get() <= sourceSize, "the decoded archive must not be traversed twice");
        assertEquals(128 * 1024, Files.size(output.resolve("bulk")));
        if (links) {
            assertTrue(Files.isSameFile(output.resolve("early"), output.resolve("alias")));
        }
        try (var files = Files.list(output)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".plexus-")));
        }
    }

    /** Compresses the same independently written TAR fixture using the requested format. */
    private Path compress(Path source, TarUnArchiver.UntarCompressionMethod method) throws Exception {
        if (method == TarUnArchiver.UntarCompressionMethod.NONE) {
            return source;
        }
        String algorithm =
                switch (method) {
                    case GZIP -> "gz";
                    case BZIP2 -> "bzip2";
                    case SNAPPY -> "snappy-framed";
                    case XZ -> "xz";
                    case ZSTD -> "zstd";
                    default -> throw new IllegalArgumentException();
                };
        Path encoded = temp.resolve("compressed.tar");
        try (var out =
                new CompressorStreamFactory().createCompressorOutputStream(algorithm, Files.newOutputStream(encoded))) {
            Files.copy(source, out);
        }
        return encoded;
    }

    /** Forward references fail immediately, even if an unrelated destination target already exists. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsForwardReferencesWithoutRetry(boolean existing) throws Exception {
        Path source = temp.resolve("forward.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "early", "first", null);
            TarHardLinkTest.entry(out, "alias", "", "later");
            TarHardLinkTest.entry(out, "later", "last", null);
        }
        Path output = Files.createDirectory(temp.resolve("output"));
        if (existing) {
            Files.writeString(output.resolve("later"), "existing");
        }
        TarUnArchiver extractor = new TarUnArchiver(source.toFile());
        extractor.setDestDirectory(output.toFile());
        assertThrows(ArchiverException.class, extractor::extract);
        assertEquals("first", Files.readString(output.resolve("early")));
        assertFalse(Files.exists(output.resolve("alias")));
        if (existing) {
            assertEquals("existing", Files.readString(output.resolve("later")));
        } else {
            assertFalse(Files.exists(output.resolve("later")));
        }
        // Finishing enumeration must not turn an invalid earlier link into a valid backward reference.
        try (TarFile reader = new TarFile(source.toFile())) {
            var entries = reader.getEntries();
            entries.nextElement();
            TarArchiveEntry link = (TarArchiveEntry) entries.nextElement();
            entries.nextElement();
            assertFalse(entries.hasMoreElements());
            assertThrows(IOException.class, () -> reader.getInputStream(link));
            assertThrows(UncheckedIOException.class, () -> new TarResource(reader, link).getSize());
        }
    }

    /** Repeated hasMoreElements calls and replay must not advance the enumeration unexpectedly. */
    @Test
    void enumeratesLazilyAndCachesOnlyRequestedLinks() throws Exception {
        Path source = temp.resolve("backward.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "file", "content", null);
            TarHardLinkTest.entry(out, "alias", "", "file");
            TarHardLinkTest.entry(out, "another", "", "alias");
        }
        AtomicInteger opens = new AtomicInteger();
        try (TarFile reader = new TarFile(source.toFile()) {
            /** Tracks replay separately from the initial enumeration stream. */
            @Override
            protected InputStream getInputStream(File file) throws IOException {
                opens.incrementAndGet();
                return super.getInputStream(file);
            }
        }) {
            var entries = reader.getEntries();
            assertEquals(1, opens.get());
            assertTrue(entries.hasMoreElements());
            assertTrue(entries.hasMoreElements());
            TarArchiveEntry first = (TarArchiveEntry) entries.nextElement();
            assertEquals("file", first.getName());
            assertEquals(1, opens.get());
            try (var contents = reader.getInputStream(first)) {
                assertEquals("content", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            TarArchiveEntry alias = (TarArchiveEntry) entries.nextElement();
            assertEquals(7, new TarResource(reader, alias).getSize());
            assertEquals(1, opens.get(), "metadata must not replay payloads");
            try (var contents = reader.getInputStream(alias)) {
                assertEquals("content", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertEquals(2, opens.get());
            TarArchiveEntry another = (TarArchiveEntry) entries.nextElement();
            assertEquals("another", another.getName());
            try (var contents = reader.getInputStream(another)) {
                assertEquals("content", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertEquals(2, opens.get(), "aliases must reuse their cached payload");
            assertFalse(entries.hasMoreElements());
            assertThrows(java.util.NoSuchElementException.class, entries::nextElement);
        }
    }

    /** An excluded target is mapped only when referenced, and the mapper result is reused. */
    @Test
    void mapsExcludedTargetOnceAndUsesExistingFile() throws Exception {
        Path source = temp.resolve("backward.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "file", "archive", null);
            TarHardLinkTest.entry(out, "alias", "", "file");
            TarHardLinkTest.entry(out, "another", "", "file");
        }
        Path output = Files.createDirectories(temp.resolve("output/mapped")).getParent();
        Files.writeString(output.resolve("mapped/file"), "existing");
        AtomicInteger mappings = new AtomicInteger();
        TarUnArchiver extractor = new TarUnArchiver(source.toFile());
        extractor.setDestDirectory(output.toFile());
        extractor.setFileSelectors(new FileSelector[] {file -> !file.getName().equals("file")});
        extractor.setFileMappers(new FileMapper[] {
            name -> {
                mappings.incrementAndGet();
                return "mapped/" + name;
            }
        });
        extractor.extract();
        assertEquals(3, mappings.get());
        assertEquals("existing", Files.readString(output.resolve("mapped/alias")));
        assertTrue(Files.isSameFile(output.resolve("mapped/file"), output.resolve("mapped/another")));
    }
    /** A second read of the current ordinary member uses replay without stealing the next header. */
    @Test
    void rereadsCurrentMemberAndFindsNamesWithoutSkipping() throws Exception {
        Path source = temp.resolve("repeat.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "first", "one", null);
            TarHardLinkTest.entry(out, "second", "two", null);
            TarHardLinkTest.entry(out, "first", "replacement", null);
        }
        try (TarFile reader = new TarFile(source.toFile())) {
            var entries = reader.getEntries();
            TarArchiveEntry first = (TarArchiveEntry) entries.nextElement();
            try (var contents = reader.getInputStream(first)) {
                assertEquals('o', contents.read());
            }
            try (var contents = reader.getInputStream(first)) {
                assertEquals("one", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            // An explicit name lookup may read ahead on its own cursor, but enumeration stays put.
            try (var contents = reader.getInputStream(new TarArchiveEntry("second"))) {
                assertEquals("two", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertEquals("second", entries.nextElement().getName());
            TarArchiveEntry replacement = (TarArchiveEntry) entries.nextElement();
            try (var contents = reader.getInputStream(replacement)) {
                assertEquals("replacement", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            try (var contents = reader.getInputStream(new TarArchiveEntry("first"))) {
                assertEquals("one", new String(contents.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertFalse(entries.hasMoreElements());
        }
    }

    /** Same-name and mapped same-path links must fail without deleting the existing target. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsSelfLinks(boolean mapped) throws Exception {
        Path source = temp.resolve("self.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "file", "content", null);
            TarHardLinkTest.entry(out, mapped ? "alias" : "file", "", "file");
        }
        Path output = Files.createDirectory(temp.resolve("output"));
        TarUnArchiver extractor = new TarUnArchiver(source.toFile());
        extractor.setDestDirectory(output.toFile());
        if (mapped) {
            extractor.setFileMappers(new FileMapper[] {name -> "file"});
        }
        assertThrows(ArchiverException.class, extractor::extract);
        assertEquals("content", Files.readString(output.resolve("file")));
    }

    /** Payload ownership cleans successful caches and incomplete copies after I/O failures. */
    @Test
    void cleansCachedPayloadsOnCloseAndFailure() throws Exception {
        Path source = temp.resolve("cache.tar");
        try (var out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "file", "content", null);
        }
        Path cached;
        try (TarFile reader = new TarFile(source.toFile());
                TarPayloads payloads = new TarPayloads()) {
            TarArchiveEntry entry = (TarArchiveEntry) reader.getEntries().nextElement();
            cached = payloads.add(reader, entry);
            assertEquals(cached, payloads.add(reader, entry));
            assertEquals("content", Files.readString(cached));
            try (TarFile failing = new TarFile(source.toFile()) {
                /** Simulates a stream-close failure after all bytes have been copied. */
                @Override
                InputStream rawContents(TarArchiveEntry ignored) {
                    return new java.io.ByteArrayInputStream(new byte[0]) {
                        /** Failing closure must not leave an untracked temporary payload. */
                        @Override
                        public void close() throws IOException {
                            throw new IOException("close failed");
                        }
                    };
                }
            }) {
                TarArchiveEntry empty = new TarArchiveEntry("empty");
                assertThrows(IOException.class, () -> payloads.add(failing, empty));
            }
            try (var files = Files.list(cached.getParent())) {
                assertEquals(1, files.count());
            }
        }
        assertFalse(Files.exists(cached));
        assertFalse(Files.exists(cached.getParent()));
    }
}
