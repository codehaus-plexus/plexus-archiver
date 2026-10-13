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
package org.codehaus.plexus.archiver.dir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.SymlinkFailureMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Copying to a directory when the file system refuses symbolic links, as Windows does without the symlink privilege.
 */
class DirectoryArchiverSymlinkFailureTest {

    @TempDir
    Path temp;

    private Path dest;

    private File file;

    private DirectoryArchiver archiver;

    @BeforeEach
    void setUp() throws IOException {
        file = Files.write(temp.resolve("file.txt"), "content".getBytes(StandardCharsets.UTF_8))
                .toFile();
        dest = temp.resolve("out");
        archiver = new DirectoryArchiver() {
            @Override
            protected void createSymbolicLink(File link, File target) throws IOException {
                throw new AccessDeniedException(link.getPath(), null, "A required privilege is not held by the client");
            }
        };
        archiver.setDestFile(dest.toFile());
    }

    @Test
    void copiesTargetInsteadOfLink() throws Exception {
        archiver.addSymlink("link", "file.txt");
        archiver.addFile(file, "file.txt");
        archiver.createArchive();

        assertThat(Files.isSymbolicLink(dest.resolve("link"))).isFalse();
        assertThat(dest.resolve("link")).hasContent("content");
    }

    @Test
    void neverCopiesFromOutsideTheDestination() throws Exception {
        archiver.addFile(file, "file.txt");
        archiver.addSymlink("escape", "../file.txt");
        archiver.createArchive();

        assertThat(dest.resolve("escape")).doesNotExist();
    }

    @Test
    void skipsWhenModeIsSkip() throws Exception {
        archiver.setSymlinkFailureMode(SymlinkFailureMode.SKIP);
        archiver.addFile(file, "file.txt");
        archiver.addSymlink("link", "file.txt");
        archiver.createArchive();

        assertThat(dest.resolve("link")).doesNotExist();
        assertThat(dest.resolve("file.txt")).hasContent("content");
    }

    @Test
    void failsWhenModeIsFail() {
        archiver.setSymlinkFailureMode(SymlinkFailureMode.FAIL);
        archiver.addFile(file, "file.txt");
        archiver.addSymlink("link", "file.txt");

        assertThatThrownBy(archiver::createArchive)
                .isInstanceOf(ArchiverException.class)
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }
}
