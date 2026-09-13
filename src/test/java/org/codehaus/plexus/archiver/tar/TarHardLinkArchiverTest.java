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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.functions.HardLinkIdentitySupplier;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.ResourceFactory;
import org.codehaus.plexus.components.io.resources.proxy.ProxyFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarHardLinkArchiverTest extends TestSupport {
    @TempDir
    Path temp;

    /** Creates real aliases, checking filesystem capability rather than assuming it from the OS. */
    Path sources() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        Path a = Files.writeString(source.resolve("a"), "content");
        try {
            Files.createLink(source.resolve("b"), a);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Hard links unavailable: " + e);
        }
        assumeTrue(
                ((HardLinkIdentitySupplier) ResourceFactory.createResource(a.toFile())).getHardLinkIdentity() != null,
                "Filesystem does not expose file keys");
        return source;
    }

    /** Writes a fresh destination while retaining the caller's configuration. */
    Path write(TarArchiver archiver) throws Exception {
        Path output = Files.createTempFile(temp, "written", ".tar");
        archiver.setDestFile(output.toFile());
        archiver.createArchive();
        return output;
    }

    /** Inspects headers and payloads with Commons Compress, independently of our extraction logic. */
    List<Header> headers(Path archive) throws IOException {
        List<Header> headers = new ArrayList<>();
        try (TarArchiveInputStream in = new TarArchiveInputStream(Files.newInputStream(archive))) {
            TarArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    headers.add(new Header(entry, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
        }
        return headers;
    }

    /** Retains decoded header metadata and bytes for assertions. */
    private record Header(TarArchiveEntry entry, String contents) {}

    /** Opt-in creation writes one full payload and uses final mapped names in later links. */
    @Test
    void createsLinksAfterMappingAndResetsBetweenArchives() throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        DefaultFileSet set = new DefaultFileSet();
        set.setDirectory(source.toFile());
        set.setPrefix("prefix/");
        set.setFileMappers(new FileMapper[] {name -> "mapped-" + name});
        for (int i = 0; i < 2; i++) {
            archiver.addFileSet(set);
            List<Header> entries = headers(write(archiver));
            assertFalse(entries.get(0).entry.isLink());
            assertEquals("content", entries.get(0).contents);
            assertTrue(entries.get(1).entry.isLink());
            assertEquals(entries.get(0).entry.getName(), entries.get(1).entry.getLinkName());
            assertEquals(0, entries.get(1).entry.getSize());
        }
    }

    /** Disabled preservation must short-circuit before consulting optional resource identity. */
    @Test
    void disabledDoesNotReadIdentity() throws Exception {
        Path file = Files.writeString(temp.resolve("file"), "content");
        PlexusIoResource original = ResourceFactory.createResource(file.toFile());
        PlexusIoResource guarded = ProxyFactory.createProxy(original, (HardLinkIdentitySupplier) () -> {
            throw new AssertionError("Identity queried with preservation disabled");
        });
        TarArchiver archiver = new TarArchiver();
        assertFalse(archiver.isPreserveHardLinks());
        archiver.addResource(guarded, "a", 0644);
        archiver.addResource(guarded, "b", 0644);
        List<Header> entries = headers(write(archiver));
        assertFalse(entries.get(1).entry.isLink());
        assertEquals("content", entries.get(1).contents);
    }

    /** Name-dependent transformations must retain each resource's distinct output bytes. */
    @Test
    void transformationsInvalidateIdentity() throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        DefaultFileSet set = new DefaultFileSet();
        set.setDirectory(source.toFile());
        set.setStreamTransformer((resource, contents) ->
                new ByteArrayInputStream((resource.getName() + "-transformed").getBytes(StandardCharsets.UTF_8)));
        archiver.addFileSet(set);
        List<Header> entries = headers(write(archiver));
        assertEquals("a-transformed", entries.get(0).contents);
        assertEquals("b-transformed", entries.get(1).contents);
        assertFalse(entries.get(1).entry.isLink());
    }

    /** Replacing TAR resource contents must preserve the replacement bytes with either writer setting. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void tarResourceSubclassesKeepReplacementContents(boolean preserve) throws Exception {
        Path source = temp.resolve("subclass.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "payload", null);
        }
        try (TarFile reader = new TarFile(source.toFile())) {
            TarArchiveEntry entry = (TarArchiveEntry) reader.getEntries().nextElement();
            TarResource original = new TarResource(reader, entry);
            TarResource changed = new TarResource(reader, entry) {
                /** Keeps size and metadata equal so only the content guarantee can distinguish the resources. */
                @Override
                public InputStream getContents() {
                    return new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8));
                }
            };
            TarArchiver archiver = new TarArchiver();
            archiver.setPreserveHardLinks(preserve);
            archiver.addResource(original, "original", 0644);
            archiver.addResource(changed, "changed", 0644);
            List<Header> entries = headers(write(archiver));
            assertEquals("payload", entries.get(0).contents);
            assertFalse(entries.get(1).entry.isLink());
            assertEquals("changed", entries.get(1).contents);
        }
    }

    /** A transparent subclass can explicitly guarantee that its inherited contents share the source payload. */
    @Test
    void tarResourceSubclassCanSupplyIdentity() throws Exception {
        Path source = temp.resolve("transparent.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "payload", null);
        }
        try (TarFile reader = new TarFile(source.toFile())) {
            TarArchiveEntry entry = (TarArchiveEntry) reader.getEntries().nextElement();
            TarResource original = new TarResource(reader, entry);
            TarResource transparent = new TarResource(reader, entry) {
                /** Delegates the guarantee to the unchanged source resource. */
                @Override
                public Object getHardLinkIdentity() throws IOException {
                    return original.getHardLinkIdentity();
                }
            };
            TarArchiver archiver = new TarArchiver();
            archiver.setPreserveHardLinks(true);
            archiver.addResource(original, "original", 0644);
            archiver.addResource(transparent, "alias", 0644);
            List<Header> entries = headers(write(archiver));
            assertEquals("payload", entries.get(0).contents);
            assertTrue(entries.get(1).entry.isLink());
            assertEquals("original", entries.get(1).entry.getLinkName());
        }
    }

    /** One inode cannot retain conflicting requested permission modes. */
    @Test
    void differentMetadataKeepsFullEntries() throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.addResource(ResourceFactory.createResource(source.resolve("a").toFile()), "a", 0600);
        archiver.addResource(ResourceFactory.createResource(source.resolve("b").toFile()), "b", 0644);
        List<Header> entries = headers(write(archiver));
        assertFalse(entries.get(1).entry.isLink());
        assertEquals("content", entries.get(1).contents);
    }

    /** Equivalent destination paths must invalidate a replaced target before a later alias is written. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"same", "./same", "dir/../same", "./dir//../same", "dir/nested/../../same", "dir/.././same"})
    void duplicateNamesInvalidateTargets(String replacementName) throws Exception {
        Path source = sources();
        Path other = Files.writeString(temp.resolve("other"), "other");
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.setDuplicateBehavior(Archiver.DUPLICATES_ADD);
        archiver.addResource(ResourceFactory.createResource(source.resolve("a").toFile()), "same", 0644);
        archiver.addResource(ResourceFactory.createResource(other.toFile()), replacementName, 0644);
        archiver.addResource(ResourceFactory.createResource(source.resolve("b").toFile()), "last", 0644);
        Path archive = write(archiver);
        List<Header> entries = headers(archive);
        assertEquals(3, entries.size());
        assertFalse(entries.get(2).entry.isLink());
        assertEquals("content", entries.get(2).contents);
        // The contained replacement path overwrites "same", but must not change the alias's original bytes.
        Path output = Files.createDirectory(temp.resolve("extracted"));
        TarUnArchiver extractor = new TarUnArchiver(archive.toFile());
        extractor.setDestDirectory(output.toFile());
        extractor.extract();
        assertEquals("other", Files.readString(output.resolve("same")));
        assertEquals("content", Files.readString(output.resolve("last")));
    }

    /** Like GNU tar, symlink traversal leaves later aliases bound to their target's current contents. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "false, redirect/file", "true, redirect/file",
        "false, ./redirect//file", "true, ./redirect//file",
        "false, redirect/../file", "true, redirect/../file"
    })
    void symlinkPathsKeepHardLinkHeaders(boolean preserve, String replacementName) throws Exception {
        Path source = sources();
        requireSymbolicLinks(source);
        Path other = Files.writeString(temp.resolve("other"), "changed");
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(preserve);
        archiver.addFile(source.resolve("a").toFile(), "real/file", 0644);
        archiver.addFile(other.toFile(), "real/sub/marker", 0644);
        archiver.addSymlink("redirect", replacementName.contains("..") ? "real/sub" : "real");
        archiver.addFile(other.toFile(), replacementName, 0644);
        archiver.addFile(source.resolve("b").toFile(), "alias", 0644);
        // An independent group after the collision must still benefit from hard-link preservation.
        Path unrelated = Files.writeString(temp.resolve("unrelated-source"), "independent");
        Path unrelatedAlias = Files.createLink(temp.resolve("unrelated-alias"), unrelated);
        archiver.addFile(unrelated.toFile(), "unrelated-first", 0644);
        archiver.addFile(unrelatedAlias.toFile(), "unrelated-second", 0644);
        Path archive = write(archiver);
        List<Header> entries = headers(archive);
        Header alias = entries.get(entries.size() - 3);
        assertEquals(preserve, alias.entry.isLink());
        assertEquals(preserve ? "real/file" : "", alias.entry.getLinkName());
        assertEquals(preserve ? "" : "content", alias.contents);
        assertFalse(entries.get(entries.size() - 2).entry.isLink());
        assertEquals(preserve, entries.get(entries.size() - 1).entry.isLink());
        assertEquals(
                preserve ? "unrelated-first" : "",
                entries.get(entries.size() - 1).entry.getLinkName());
        Path output = Files.createDirectory(temp.resolve("extracted"));
        TarUnArchiver extractor = new TarUnArchiver(archive.toFile());
        extractor.setDestDirectory(output.toFile());
        extractor.extract();
        assertEquals("changed", Files.readString(output.resolve("real/file")));
        assertEquals(preserve ? "changed" : "content", Files.readString(output.resolve("alias")));
        assertEquals(preserve, Files.isSameFile(output.resolve("real/file"), output.resolve("alias")));
        assertEquals(preserve, Files.isSameFile(output.resolve("unrelated-first"), output.resolve("unrelated-second")));

        // Reusing the writer must not retain names from the completed archive.
        archiver.addFile(source.resolve("a").toFile(), "first", 0644);
        archiver.addFile(source.resolve("b").toFile(), "second", 0644);
        List<Header> next = headers(write(archiver));
        assertEquals(preserve, next.get(1).entry.isLink());
    }

    /** Nested directory symlinks also retain pathname-based hard links, as with GNU tar. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void symlinksThroughSymlinksKeepHardLinkHeaders(boolean preserve) throws Exception {
        Path source = sources();
        requireSymbolicLinks(source);
        Path other = Files.writeString(temp.resolve("other"), "changed");
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(preserve);
        archiver.addFile(other.toFile(), "real/marker", 0644);
        archiver.addSymlink("redirect", "real");
        archiver.addSymlink("redirect/nested", "../real");
        archiver.addFile(source.resolve("a").toFile(), "real/file", 0644);
        archiver.addFile(other.toFile(), "real/nested/file", 0644);
        archiver.addFile(source.resolve("b").toFile(), "alias", 0644);
        Path archive = write(archiver);
        List<Header> entries = headers(archive);
        assertEquals(preserve, entries.get(entries.size() - 1).entry.isLink());
        assertEquals(
                preserve ? "real/file" : "",
                entries.get(entries.size() - 1).entry.getLinkName());
        assertEquals(preserve ? "" : "content", entries.get(entries.size() - 1).contents);
        Path output = Files.createDirectory(temp.resolve("extracted"));
        TarUnArchiver extractor = new TarUnArchiver(archive.toFile());
        extractor.setDestDirectory(output.toFile());
        extractor.extract();
        assertEquals("changed", Files.readString(output.resolve("real/file")));
        assertEquals(preserve ? "changed" : "content", Files.readString(output.resolve("alias")));
        assertEquals(preserve, Files.isSameFile(output.resolve("real/file"), output.resolve("alias")));
    }

    /** A symlink on an unrelated path must not disable preservation for ordinary members. */
    @Test
    void unrelatedSymlinksAllowPreservation() throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.addFile(source.resolve("a").toFile(), "first", 0644);
        archiver.addSymlink("unrelated", "elsewhere");
        archiver.addFile(source.resolve("b").toFile(), "second", 0644);
        List<Header> entries = headers(write(archiver));
        assertEquals("content", entries.get(0).contents);
        assertTrue(entries.get(1).entry.isSymbolicLink());
        assertTrue(entries.get(2).entry.isLink());
        assertEquals("first", entries.get(2).entry.getLinkName());
    }

    /** Checks symbolic-link capability on the test filesystem before exercising extraction through a directory link. */
    private void requireSymbolicLinks(Path directory) throws IOException {
        try {
            Files.createSymbolicLink(directory.resolve("symlink-probe"), Path.of("."));
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
    }

    /** A failure after writing one file cannot leak its target identity into the next archive. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void failedArchiveDoesNotLeakTargets(boolean symlinkTraversal) throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.addResource(ResourceFactory.createResource(source.resolve("a").toFile()), "first", 0644);
        if (symlinkTraversal) {
            archiver.addSymlink("redirect", ".");
            archiver.addFile(source.resolve("a").toFile(), "redirect/through", 0644);
        }
        Path missing = Files.writeString(temp.resolve("missing"), "missing");
        archiver.addResource(ResourceFactory.createResource(missing.toFile()), "missing", 0644);
        Files.delete(missing);
        // The missing file can fail during identity lookup or payload copying; both must reset writer state.
        Exception failure = assertThrows(Exception.class, () -> write(archiver));
        assertTrue(failure instanceof ArchiverException || failure instanceof IOException);
        archiver.addResource(ResourceFactory.createResource(source.resolve("b").toFile()), "next", 0644);
        archiver.addResource(ResourceFactory.createResource(source.resolve("a").toFile()), "next-alias", 0644);
        List<Header> entries = headers(write(archiver));
        assertFalse(entries.get(0).entry.isLink());
        assertEquals("content", entries.get(0).contents);
        assertTrue(entries.get(1).entry.isLink());
        assertEquals("next", entries.get(1).entry.getLinkName());
    }

    /** Excluding the first alias must cause the remaining selected name to carry the payload. */
    @Test
    void excludedFirstAliasIsNotALinkTarget() throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        DefaultFileSet set = new DefaultFileSet();
        set.setDirectory(source.toFile());
        set.setExcludes(new String[] {"a"});
        archiver.addFileSet(set);
        List<Header> entries = headers(write(archiver));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).entry.isLink());
        assertEquals("content", entries.get(0).contents);
    }

    /** PAX and GNU long-link extensions must name the same full member written earlier. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = TarLongFileMode.class,
            names = {"posix", "gnu"})
    void supportsLongLinkNames(TarLongFileMode mode) throws Exception {
        Path source = sources();
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.setLongfile(mode);
        String name = "long/" + "x".repeat(150);
        archiver.addResource(ResourceFactory.createResource(source.resolve("a").toFile()), name, 0644);
        archiver.addResource(ResourceFactory.createResource(source.resolve("b").toFile()), "short", 0644);
        List<Header> entries = headers(write(archiver));
        assertTrue(entries.get(1).entry.isLink());
        assertEquals(name, entries.get(1).entry.getLinkName());
    }

    /** Archive-resource wrappers preserve identities through mapping, including backward aliases. */
    @Test
    void repacksSelectedAliasesThroughArchivedFileSet() throws Exception {
        Path source = Files.createTempFile(temp, "source", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "content", null);
            TarHardLinkTest.entry(out, "a", "", "data");
            TarHardLinkTest.entry(out, "b", "", "data");
        }
        TarArchiver archiver = (TarArchiver) lookup(Archiver.class, "tar");
        archiver.setPreserveHardLinks(true);
        DefaultArchivedFileSet set = new DefaultArchivedFileSet(source.toFile());
        set.setExcludes(new String[] {"data"});
        set.setFileMappers(new FileMapper[] {name -> "mapped/" + name});
        archiver.addArchivedFileSet(set);
        List<Header> entries = headers(write(archiver));
        assertEquals("mapped/a", entries.get(0).entry.getName());
        assertEquals("content", entries.get(0).contents);
        assertTrue(entries.get(1).entry.isLink());
        assertEquals("mapped/a", entries.get(1).entry.getLinkName());
    }

    /** Formats without native links receive each alias's logical bytes and size. */
    @Test
    void convertsSelectedAliasToZip() throws Exception {
        Path source = Files.createTempFile(temp, "source", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(source))) {
            TarHardLinkTest.entry(out, "data", "content", null);
            TarHardLinkTest.entry(out, "alias", "", "data");
        }
        Archiver archiver = lookup(Archiver.class, "zip");
        DefaultArchivedFileSet set = new DefaultArchivedFileSet(source.toFile());
        set.setIncludes(new String[] {"alias"});
        archiver.addArchivedFileSet(set);
        Path output = temp.resolve("converted.zip");
        archiver.setDestFile(output.toFile());
        archiver.createArchive();
        try (var zip = new java.util.zip.ZipFile(output.toFile())) {
            assertEquals(1, zip.size());
            assertEquals(7, zip.getEntry("alias").getSize());
            assertEquals(
                    "content",
                    new String(zip.getInputStream(zip.getEntry("alias")).readAllBytes(), StandardCharsets.UTF_8));
        }
    }
    /** Unknown identities and replacement content suppliers remain ordinary full entries. */
    @Test
    void unknownIdentityAndCustomContentsAreNotLinked() throws Exception {
        Path file = Files.writeString(temp.resolve("file"), "disk");
        PlexusIoResource resource = ResourceFactory.createResource(file.toFile());
        PlexusIoResource unknown = ProxyFactory.createProxy(resource, (HardLinkIdentitySupplier) () -> null);
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.addResource(unknown, "a", 0644);
        archiver.addResource(unknown, "b", 0644);
        archiver.addResource(
                ResourceFactory.createResource(
                        file.toFile(),
                        "c",
                        () -> new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)),
                        (org.codehaus.plexus.components.io.functions.InputStreamTransformer) null),
                "c",
                0644);
        List<Header> entries = headers(write(archiver));
        assertTrue(entries.stream().noneMatch(header -> header.entry.isLink()));
        assertEquals("disk", entries.get(1).contents);
        assertEquals("data", entries.get(2).contents);
    }

    /** Symbolic links retain their own header type instead of inheriting the target's file key. */
    @Test
    void symbolicLinksAreNotHardLinks() throws Exception {
        Path source = sources();
        try {
            Files.createSymbolicLink(source.resolve("symlink"), Path.of("a"));
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        TarArchiver archiver = new TarArchiver();
        archiver.setPreserveHardLinks(true);
        archiver.addFileSet(DefaultFileSet.fileSet(source.toFile()));
        List<Header> entries = headers(write(archiver));
        assertEquals(
                1,
                entries.stream().filter(header -> header.entry.isSymbolicLink()).count());
        assertEquals(1, entries.stream().filter(header -> header.entry.isLink()).count());
    }
}
