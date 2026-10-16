package org.codehaus.plexus.archivers.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for ReproducibleBuildConfigurer.
 */
class ReproducibleBuildConfigurerIT {

    @Test
    void reproducibleBuildSetsDeterministicContentAndTimestamp(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("b.txt"), "bravo");
        Files.writeString(sourceDir.resolve("a.txt"), "alpha");
        Files.createDirectories(sourceDir.resolve("sub"));
        Files.writeString(sourceDir.resolve("sub/c.txt"), "charlie");

        Path archivePath = tempDir.resolve("archive.zip");
        FileTime ts = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));

        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);
        configurer.resource(ResourceScan.fromDirectory(sourceDir), emit -> emit.prefix("root/"));

        configurer.configureReproducibleBuild(rb -> {
            rb.setDefaultLastModifiedTime(ts);
            rb.setFilenameComparator(String::compareTo);
            rb.setDefaultUid(0);
            rb.setDefaultUserName("root");
            rb.setDefaultGid(0);
            rb.setDefaultGroupName("root");
        });

        archiver.createArchive();

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertFileContents(
                    zip,
                    Map.of(
                            "root/a.txt", "alpha",
                            "root/b.txt", "bravo",
                            "root/sub/c.txt", "charlie"));

            // Verify timestamp was normalized for all file entries
            LocalDateTime expectedZipTime = LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC);
            
            for (String entryName : List.of("root/a.txt", "root/b.txt", "root/sub/c.txt")) {
                ZipEntry entry = zip.getEntry(entryName);
                assertThat(entry).isNotNull();
                assertThat(entry.getTimeLocal()).isEqualTo(expectedZipTime);
            }

            // Verify deterministic central-directory order from comparator
            assertThat(entryOrder(zip))
                    .containsSubsequence("root/a.txt", "root/b.txt", "root/sub/c.txt");
        }
    }

    @Test
    void reproducibleBuildByteForByteDeterministic(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("z.txt"), "z");
        Files.writeString(sourceDir.resolve("a.txt"), "a");
        Files.writeString(sourceDir.resolve("m.txt"), "m");

        FileTime ts = FileTime.from(Instant.parse("2021-06-15T12:30:45Z"));

        Path archive1 = tempDir.resolve("archive-1.zip");
        Path archive2 = tempDir.resolve("archive-2.zip");

        createReproducibleZip(sourceDir, archive1, ts);
        createReproducibleZip(sourceDir, archive2, ts);

        byte[] bytes1 = Files.readAllBytes(archive1);
        byte[] bytes2 = Files.readAllBytes(archive2);

        assertThat(bytes1).isEqualTo(bytes2);

        // Also check logical content
        try (ZipFile zip = new ZipFile(archive1.toFile())) {
            assertFileContents(
                    zip,
                    Map.of(
                            "a.txt", "a",
                            "m.txt", "m",
                            "z.txt", "z"));
            assertThat(entryOrder(zip)).containsSubsequence("a.txt", "m.txt", "z.txt");
        }
    }

    @Test
    void reproducibleBuildNoopConsumerStillProducesExpectedContent(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "content");

        Path archivePath = tempDir.resolve("archive.zip");

        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);
        configurer.resource(ResourceScan.fromDirectory(sourceDir), emit -> {});

        configurer.configureReproducibleBuild(rb -> {
            // intentionally empty
        });

        archiver.createArchive();

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertFileContents(zip, Map.of("file.txt", "content"));
        }
    }

    private static void createReproducibleZip(Path sourceDir, Path archivePath, FileTime ts) throws Exception {
        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);
        configurer.resource(ResourceScan.fromDirectory(sourceDir), emit -> {});

        configurer.configureReproducibleBuild(rb -> {
            rb.setDefaultLastModifiedTime(ts);
            rb.setFilenameComparator(String::compareTo);
            rb.setDefaultUid(0);
            rb.setDefaultUserName("root");
            rb.setDefaultGid(0);
            rb.setDefaultGroupName("root");
        });

        archiver.createArchive();
    }

    private static void assertFileContents(ZipFile zip, Map<String, String> expected) throws IOException {
        Map<String, byte[]> actual = new LinkedHashMap<>();

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                actual.put(entry.getName(), in.readAllBytes());
            }
        }

        assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(expected.keySet());

        for (Map.Entry<String, String> expectedEntry : expected.entrySet()) {
            assertThat(actual.get(expectedEntry.getKey()))
                    .as("content of %s", expectedEntry.getKey())
                    .isEqualTo(expectedEntry.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<String> entryOrder(ZipFile zip) {
        return zip.stream().map(ZipEntry::getName).toList();
    }
}