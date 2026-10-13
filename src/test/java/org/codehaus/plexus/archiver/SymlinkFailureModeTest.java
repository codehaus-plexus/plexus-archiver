/*
 * Copyright The Plexus developers.
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Extraction when the file system refuses symbolic links, as Windows does without the symlink privilege.
 */
class SymlinkFailureModeTest {

    @TempDir
    Path temp;

    private Path archive;

    private Path dest;

    /** Builds a tar from alternating entries: a name, then its content, or "->target" for a symbolic link. */
    private File tar(String... entries) throws IOException {
        archive = temp.resolve("test.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            for (int i = 0; i < entries.length; i += 2) {
                String name = entries[i];
                String value = entries[i + 1];
                TarArchiveEntry entry;
                byte[] content = new byte[0];
                if (value.startsWith("->")) {
                    entry = new TarArchiveEntry(name, TarArchiveEntry.LF_SYMLINK);
                    entry.setLinkName(value.substring(2));
                } else if (name.endsWith("/")) {
                    entry = new TarArchiveEntry(name);
                } else {
                    entry = new TarArchiveEntry(name);
                    content = value.getBytes(StandardCharsets.UTF_8);
                    entry.setSize(content.length);
                }
                out.putArchiveEntry(entry);
                out.write(content);
                out.closeArchiveEntry();
            }
        }
        return archive.toFile();
    }

    private File zip(String... entries) throws IOException {
        archive = temp.resolve("test.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            for (int i = 0; i < entries.length; i += 2) {
                String name = entries[i];
                String value = entries[i + 1];
                ZipArchiveEntry entry = new ZipArchiveEntry(name);
                byte[] content;
                if (value.startsWith("->")) {
                    entry.setUnixMode(UnixStat.LINK_FLAG | 0777);
                    content = value.substring(2).getBytes(StandardCharsets.UTF_8);
                } else {
                    content = value.getBytes(StandardCharsets.UTF_8);
                }
                out.putArchiveEntry(entry);
                out.write(content);
                out.closeArchiveEntry();
            }
        }
        return archive.toFile();
    }

    /** Throws what Windows throws without the privilege. */
    private static void refuse(File link) throws IOException {
        throw new AccessDeniedException(link.getPath(), null, "A required privilege is not held by the client");
    }

    private TarUnArchiver refusingTar(File source) throws IOException {
        TarUnArchiver unArchiver = new TarUnArchiver(source) {
            @Override
            protected void createSymbolicLink(File link, File target) throws IOException {
                refuse(link);
            }
        };
        dest = Files.createDirectories(temp.resolve("out"));
        unArchiver.setDestDirectory(dest.toFile());
        return unArchiver;
    }

    private String read(String name) throws IOException {
        return new String(Files.readAllBytes(dest.resolve(name)), StandardCharsets.UTF_8);
    }

    private void assertCopied(String name, String content) throws IOException {
        assertThat(dest.resolve(name)).isRegularFile();
        assertThat(Files.isSymbolicLink(dest.resolve(name))).isFalse();
        assertThat(read(name)).isEqualTo(content);
    }

    @Test
    void copiesTargetWhenLinkFollowsIt() throws Exception {
        refusingTar(tar("file.txt", "content", "link", "->file.txt")).extract();

        assertCopied("link", "content");
    }

    @Test
    void copiesTargetWhenLinkPrecedesIt() throws Exception {
        refusingTar(tar("link", "->file.txt", "file.txt", "content")).extract();

        assertCopied("link", "content");
    }

    @Test
    void copiesTargetInSubdirectory() throws Exception {
        refusingTar(tar("lib/server/libjsig.so", "->../libjsig.so", "lib/libjsig.so", "so"))
                .extract();

        assertCopied("lib/server/libjsig.so", "so");
    }

    @Test
    void resolvesChainsInAnyOrder() throws Exception {
        refusingTar(tar("a", "->b", "b", "->file.txt", "file.txt", "content")).extract();

        assertCopied("a", "content");
        assertCopied("b", "content");
    }

    @Test
    void skipsCycles() throws Exception {
        refusingTar(tar("a", "->b", "b", "->a")).extract();

        assertThat(dest.resolve("a")).doesNotExist();
        assertThat(dest.resolve("b")).doesNotExist();
    }

    @Test
    void neverCopiesFromOutsideTheDestination() throws Exception {
        Files.write(temp.resolve("outside.txt"), "secret".getBytes(StandardCharsets.UTF_8));

        refusingTar(tar(
                        "parent", "->../outside.txt",
                        "nested", "->sub/../../outside.txt",
                        "absolute", "->" + temp.resolve("outside.txt").toAbsolutePath()))
                .extract();

        assertThat(dest.resolve("parent")).doesNotExist();
        assertThat(dest.resolve("nested")).doesNotExist();
        assertThat(dest.resolve("absolute")).doesNotExist();
    }

    @Test
    void copiesDirectoryTargets() throws Exception {
        refusingTar(tar("dir/", "", "dir/inner.txt", "inner", "dirLink", "->dir"))
                .extract();

        assertCopied("dirLink/inner.txt", "inner");
    }

    @Test
    void skipsLinksToAnEnclosingDirectory() throws Exception {
        refusingTar(tar("dir/", "", "dir/self", "->.", "dir/up", "->..")).extract();

        assertThat(dest.resolve("dir/self")).doesNotExist();
        assertThat(dest.resolve("dir/up")).doesNotExist();
    }

    @Test
    void skipsWhenModeIsSkip() throws Exception {
        TarUnArchiver unArchiver = refusingTar(tar("file.txt", "content", "link", "->file.txt"));
        unArchiver.setSymlinkFailureMode(SymlinkFailureMode.SKIP);
        unArchiver.extract();

        assertThat(dest.resolve("link")).doesNotExist();
        assertCopied("file.txt", "content");
    }

    @Test
    void failsWhenModeIsFail() throws Exception {
        TarUnArchiver unArchiver = refusingTar(tar("file.txt", "content", "link", "->file.txt"));
        unArchiver.setSymlinkFailureMode(SymlinkFailureMode.FAIL);

        assertThatThrownBy(unArchiver::extract)
                .isInstanceOf(ArchiverException.class)
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    void extractsTwiceIntoTheSameDirectory() throws Exception {
        TarUnArchiver unArchiver = refusingTar(tar("link", "->file.txt", "file.txt", "content"));
        unArchiver.extract();
        unArchiver.extract();

        assertCopied("link", "content");
    }

    @Test
    void copiesZipLinkTargets() throws Exception {
        ZipUnArchiver unArchiver = new ZipUnArchiver(zip("link", "->file.txt", "file.txt", "content")) {
            @Override
            protected void createSymbolicLink(File link, File target) throws IOException {
                refuse(link);
            }
        };
        dest = Files.createDirectories(temp.resolve("out"));
        unArchiver.setDestDirectory(dest.toFile());
        unArchiver.extract();

        assertCopied("link", "content");
    }

    @Test
    void defaultsToCopyTarget() {
        assertThat(new TarUnArchiver().getSymlinkFailureMode()).isEqualTo(SymlinkFailureMode.COPY_TARGET);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void createsLinksWhenTheFileSystemAllowsThem() throws Exception {
        TarUnArchiver unArchiver = new TarUnArchiver(tar("link", "->file.txt", "file.txt", "content"));
        dest = Files.createDirectories(temp.resolve("out"));
        unArchiver.setDestDirectory(dest.toFile());
        unArchiver.extract();

        assertThat(Files.isSymbolicLink(dest.resolve("link"))).isTrue();
        assertThat(Files.readSymbolicLink(dest.resolve("link")).toString()).isEqualTo("file.txt");
    }
}
