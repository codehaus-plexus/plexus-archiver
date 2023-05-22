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
package org.codehaus.plexus.archiver.manager;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dan T. Tran
 */
class ArchiverManagerTest extends TestSupport {

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
    void testReuseArchiver() throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);

        Archiver archiver = manager.getArchiver("jar");
        assertNotNull(archiver);

        archiver.addDirectory(new File(getBasedir()));

        Archiver newArchiver = manager.getArchiver("jar");
        assertNotNull(newArchiver);
        assertFalse(newArchiver.equals(archiver));

        assertTrue(!newArchiver.getResources().hasNext());
    }

    @Test
    void allArchiversShouldBeUnderTest() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThat(manager.getAvailableArchivers())
                .containsExactlyInAnyOrderElementsOf(getArchiversForTests().collect(Collectors.toList()));
    }

    @Test
    void allUnArchiversShouldBeUnderTest() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThat(manager.getAvailableUnArchivers())
                .containsExactlyInAnyOrderElementsOf(getUnArchiversForTests().collect(Collectors.toList()));
    }

    @Test
    void allResourceCollectionsShouldBeUnderTest() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThat(manager.getAvailableResourceCollections())
                .containsExactlyInAnyOrderElementsOf(
                        getResourceCollectionsForTests().collect(Collectors.toList()));
    }

    @ParameterizedTest
    @MethodSource("getArchiversForTests")
    void testLookupArchiver(String archiveName) throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);
        Archiver archiver = manager.getArchiver(archiveName);

        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getUnArchiversForTests")
    void testLookupUnArchiver(String archiveName) throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);
        UnArchiver archiver = manager.getUnArchiver(archiveName);

        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getResourceCollectionsForTests")
    void testLookupResourceCollection(String resourceName) throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);
        PlexusIoResourceCollection resourceCollection = manager.getResourceCollection(resourceName);

        assertThat(resourceCollection).isNotNull();
    }

    @Test
    void testLookupUnknownArchiver() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getArchiver("Unknown"));
    }

    @Test
    void testLookupUnknownUnArchiver() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getUnArchiver("Unknown"));
    }

    @Test
    void testLookupUnknownResourceCollection() {
        ArchiverManager manager = lookup(ArchiverManager.class);

        assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getResourceCollection("Unknown"));
    }

    @ParameterizedTest
    @MethodSource("getUnArchiversForTests")
    void testLookupUnArchiverUsingFile(String archiveName) throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);

        UnArchiver archiver = manager.getUnArchiver(new File("test", "test." + archiveName));
        assertThat(archiver).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("getArchiversForTests")
    void testLookupArchiverUsingFile(String archiveName) throws Exception {
        ArchiverManager manager = lookup(ArchiverManager.class);

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
    void testUnsupportedLookupArchiverUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = lookup(ArchiverManager.class);

        NoSuchArchiverException exception = assertThrowsExactly(
                NoSuchArchiverException.class, () -> manager.getArchiver(new File("test", fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }

    @ParameterizedTest
    @MethodSource("getUnsupportedFiles")
    void testUnsupportedLookupUnArchiverUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = lookup(ArchiverManager.class);

        NoSuchArchiverException exception =
                assertThrowsExactly(NoSuchArchiverException.class, () -> manager.getUnArchiver(new File(fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }

    @ParameterizedTest
    @MethodSource("getUnsupportedFiles")
    void testUnsupportedLookupResourceCollectionUsingFile(String fileName, String fileExtension) {
        ArchiverManager manager = lookup(ArchiverManager.class);

        NoSuchArchiverException exception = assertThrowsExactly(
                NoSuchArchiverException.class, () -> manager.getResourceCollection(new File(fileName)));

        assertThat(exception.getArchiver()).isEqualTo(fileExtension);
    }
}
