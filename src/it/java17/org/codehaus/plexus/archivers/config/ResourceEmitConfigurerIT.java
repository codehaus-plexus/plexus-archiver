package org.codehaus.plexus.archivers.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for ResourceEmitConfigurer behavior that is currently supported
 * by file-set based resource emission.
 */
class ResourceEmitConfigurerIT {

    @Test
    void perResourcePrefixApplies(@TempDir Path tempDir) throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir1.resolve("file.txt"), "content1");
        Files.writeString(dir2.resolve("file.txt"), "content2");

        Path archivePath = tempDir.resolve("archive.zip");

        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);

        configurer.resource(
                ResourceScan.fromDirectory(dir1),
                emit -> emit.prefix("first/"));

        configurer.resource(
                ResourceScan.fromDirectory(dir2),
                emit -> emit.prefix("second/"));

        archiver.createArchive();

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertFileContents(
                    zip,
                    Map.of(
                            "first/file.txt", "content1",
                            "second/file.txt", "content2"));
        }
    }

    @Test
    void perResourceEmptyDirectoryHandling(@TempDir Path tempDir) throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1.resolve("empty"));
        Files.createDirectories(dir2.resolve("empty"));
        Files.writeString(dir1.resolve("file.txt"), "content");
        Files.writeString(dir2.resolve("file.txt"), "content");

        Path archivePath = tempDir.resolve("archive.zip");

        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);

        // First resource: include empty directories
        configurer.resource(
                ResourceScan.fromDirectory(dir1),
                emit -> {
                    emit.prefix("with-empty/");
                    emit.setEmptyDirectoryHandling(EmptyDirectoryHandling.INCLUDE);
                });

        // Second resource: exclude empty directories
        configurer.resource(
                ResourceScan.fromDirectory(dir2),
                emit -> {
                    emit.prefix("no-empty/");
                    emit.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
                });

        archiver.createArchive();

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertFileContents(
                    zip,
                    Map.of(
                            "with-empty/file.txt", "content",
                            "no-empty/file.txt", "content"));

            assertDirectory(zip, "with-empty/empty/");
            assertThat(zip.getEntry("no-empty/empty/")).isNull();
        }
    }

    @Test
    void multipleResourcesWithDifferentPrefixesAndContents(@TempDir Path tempDir) throws Exception {
        Path data = tempDir.resolve("data");
        Path config = tempDir.resolve("config");
        Path scripts = tempDir.resolve("scripts");

        Files.createDirectories(data);
        Files.createDirectories(config);
        Files.createDirectories(scripts);

        Files.writeString(data.resolve("data.json"), "{}");
        Files.writeString(config.resolve("app.conf"), "setting=value");
        Files.writeString(scripts.resolve("build.sh"), "#!/bin/bash");

        Path archivePath = tempDir.resolve("archive.zip");

        Archiver archiver = new ZipArchiver();
        ArchiverConfigurer configurer = ArchiverConfigurer.of(archiver);
        configurer.setDestFile(archivePath);

        configurer.resource(
                ResourceScan.fromDirectory(data),
                emit -> emit.prefix("data/"));

        configurer.resource(
                ResourceScan.fromDirectory(config),
                emit -> emit.prefix("config/"));

        configurer.resource(
                ResourceScan.fromDirectory(scripts),
                emit -> emit.prefix("scripts/"));

        archiver.createArchive();

        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            assertFileContents(
                    zip,
                    Map.of(
                            "data/data.json", "{}",
                            "config/app.conf", "setting=value",
                            "scripts/build.sh", "#!/bin/bash"));
        }
    }

    private static void assertFileContents(ZipFile zip, Map<String, String> expected) throws IOException {
        assertThat(zip.stream()
                        .filter(entry -> !entry.isDirectory())
                        .map(ZipEntry::getName)
                        .toList())
                .containsExactlyInAnyOrderElementsOf(expected.keySet());

        for (Map.Entry<String, String> file : expected.entrySet()) {
            ZipEntry entry = zip.getEntry(file.getKey());
            assertThat(entry).as("entry %s", file.getKey()).isNotNull();
            assertThat(entry.isDirectory()).isFalse();

            try (InputStream input = zip.getInputStream(entry)) {
                assertThat(input.readAllBytes())
                        .as("contents of %s", file.getKey())
                        .isEqualTo(file.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void assertDirectory(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        assertThat(entry).as("directory %s", name).isNotNull();
        assertThat(entry.isDirectory()).isTrue();
    }
}