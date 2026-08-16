/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.codehaus.plexus.archiver.manager;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 *This is a copy of ArchiverManager, but without the {@code extends TestSupport} and always using
 * {@code SpiArchiverManager} as the ArchiverManager
 */
class ServiceLoaderArchiverManagerTest {

    // list of items which support Archiver and UnArchiver
    private static Stream<String> getArchiversAndUnArchiverForTests() {
        return Stream.of(
                "bzip2",
                "ear",
                "gzip",
                "jar",
                "rar",
                "tar",
                "tar.bz2",
                "tar.gz",
                "tar.snappy",
                "tar.xz",
                "tar.zst",
                "tbz2",
                "tgz",
                "txz",
                "tzst",
                "war",
                "xz",
                "zip",
                "snappy",
                "zst");
    }

    // list of items which support UnArchiver
    private static Stream<String> getUnArchiversForTests() {
        return Stream.concat(
                getArchiversAndUnArchiverForTests(),
                Stream.of(
                        // only UnArchivers
                        "car", "esb", "nar", "par", "sar", "swc"));
    }

    // list of Archiver
    private static Stream<String> getArchiversForTests() {
        return Stream.concat(
                getArchiversAndUnArchiverForTests(),
                Stream.of(
                        // only Archivers
                        "dir", "mjar"));
    }

    private static Stream<String> getResourceCollectionsForTests() {
        return Stream.concat(
                getUnArchiversForTests(),
                Stream.of(
                        "default", "files", /* defined in plexus-io */
                        "gz", "bz2" /* additional alias only for it */));
    }

    @Test
    void reuseArchiver(@TempDir File tempDir) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        Archiver archiver = manager.getArchiver("jar");
        assertNotNull(archiver);

        archiver.addFileSet(DefaultFileSet.fileSet(tempDir));

        Archiver newArchiver = manager.getArchiver("jar");
        assertNotNull(newArchiver);
        assertNotEquals(newArchiver, archiver);

        assertFalse(newArchiver.getResources().hasNext());
    }

    @Test
    void allArchiversShouldBeUnderTest() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThat(manager.getAvailableArchivers())
                .containsExactlyInAnyOrderElementsOf(getArchiversForTests().collect(Collectors.toList()));
    }

    @Test
    void allUnArchiversShouldBeUnderTest() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThat(manager.getAvailableUnArchivers())
                .containsExactlyInAnyOrderElementsOf(getUnArchiversForTests().collect(Collectors.toList()));
    }

    @Test
    void allResourceCollectionsShouldBeUnderTest() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThat(manager.getAvailableResourceCollections())
                .containsExactlyInAnyOrderElementsOf(
                        getResourceCollectionsForTests().collect(Collectors.toList()));
    }

    @ParameterizedTest
    @MethodSource("getArchiversForTests")
    void lookupArchiver(String archiveName) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();
        Archiver archiver = manager.getArchiver(archiveName);

        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getUnArchiversForTests")
    void lookupUnArchiver(String archiveName) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();
        UnArchiver archiver = manager.getUnArchiver(archiveName);

        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getResourceCollectionsForTests")
    void lookupResourceCollection(String resourceName) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();
        PlexusIoResourceCollection resourceCollection = manager.getResourceCollection(resourceName);

        assertThat(resourceCollection).isNotNull();
    }

    @Test
    void lookupUnknownArchiver() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getArchiver("Unknown"));
    }

    @Test
    void lookupUnknownUnArchiver() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getUnArchiver("Unknown"));
    }

    @Test
    void lookupUnknownResourceCollection() {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getResourceCollection("Unknown"));
    }

    @ParameterizedTest
    @MethodSource("getUnArchiversForTests")
    void lookupUnArchiverUsingFile(String archiveName) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        UnArchiver archiver = manager.getUnArchiver(new File("test", "test." + archiveName));
        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getArchiversForTests")
    void lookupArchiverUsingFile(String archiveName) throws Exception {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        Archiver archiver = manager.getArchiver(new File("test." + archiveName));
        assertThat(archiver).isNotNull();
    }

    private static Stream<Arguments> getUnsupportedFiles() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("test", ""),
                Arguments.of("test.xxx", "xxx"),
                Arguments.of("test.tar.xxx", "tar.xxx"),
                Arguments.of("tar.gz.xxx", "xxx"));
    }

    @ParameterizedTest
    @MethodSource("getUnsupportedFiles")
    void unsupportedLookupArchiverUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        NoSuchArchiverException exception = assertThrowsExactly(
                NoSuchArchiverException.class, () -> manager.getArchiver(new File("test", fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }

    @ParameterizedTest
    @MethodSource("getUnsupportedFiles")
    void unsupportedLookupUnArchiverUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        NoSuchArchiverException exception =
                assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getUnArchiver(new File(fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }

    @ParameterizedTest
    @MethodSource("getUnsupportedFiles")
    void unsupportedLookupResourceCollectionUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = new ServiceLoaderArchiverManager();

        NoSuchArchiverException exception = assertThrowsExactly(
                NoSuchArchiverException.class, () -> manager.getResourceCollection(new File(fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }
}
