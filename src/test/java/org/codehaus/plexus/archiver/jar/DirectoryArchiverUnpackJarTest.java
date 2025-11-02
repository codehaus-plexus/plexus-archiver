package org.codehaus.plexus.archiver.jar;

import javax.annotation.Nonnull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryArchiverUnpackJarTest extends TestSupport {

    public static final String[] DEFAULT_INCLUDES_ARRAY = {"**/*"};

    static class IdentityTransformer implements InputStreamTransformer {

        IdentityTransformer() {}

        @Nonnull
        @Override
        public InputStream transform(@Nonnull PlexusIoResource resource, @Nonnull InputStream inputStream)
                throws IOException {
            return inputStream;
        }
    }

    @Test
    void dependency_sets_dep_set_unpacked_rdonly() throws Exception {
        File src = new File("src/test/resources/unpack_issue.jar");
        File dest = new File("target/depset_unpack");
        FileUtils.deleteDirectory(dest);
        assertThat(src).isFile();

        Archiver archiver = createArchiver(src, dest);
        archiver.setDefaultDirectoryMode(0555);
        archiver.setDirectoryMode(0555); // causes permission denied if bug is not fixed.
        archiver.createArchive();
        assertThat(new File(dest, "child-1/META-INF/MANIFEST.MF")).isFile();

        // make them writeable or mvn clean will fail
        ArchiveEntryUtils.chmod(new File(dest, "child-1/META-INF"), 0777);
        ArchiveEntryUtils.chmod(new File(dest, "child-1/META-INF/maven"), 0777);
        ArchiveEntryUtils.chmod(new File(dest, "child-1/META-INF/maven/test"), 0777);
        ArchiveEntryUtils.chmod(new File(dest, "child-1/META-INF/maven/test/child1"), 0777);
        ArchiveEntryUtils.chmod(new File(dest, "child-1/assembly-resources"), 0777);
    }

    @Test
    void dependency_sets_dep_set_unpacked_by_default_dont_override() throws Exception {

        File src = new File("src/test/resources/unpack_issue.jar");
        File dest = new File("target/depset_unpack_dont_override");
        FileUtils.deleteDirectory(dest);

        Archiver archiver = createArchiver(src, dest);
        archiver.createArchive();

        File manifestFile = new File(dest, "child-1/META-INF/MANIFEST.MF");
        assertThat(manifestFile).content().hasLineCount(6);

        // change content of one file
        overwriteFileContent(manifestFile.toPath());
        assertThat(manifestFile).content().hasLineCount(1);

        archiver = createArchiver(src, dest);
        archiver.createArchive();

        // content was not changed
        assertThat(manifestFile).content().hasLineCount(1);
    }

    @Test
    void dependency_sets_dep_set_force_unpacked() throws Exception {

        File src = new File("src/test/resources/unpack_issue.jar");
        File dest = new File("target/depset_unpack_force");
        FileUtils.deleteDirectory(dest);

        Archiver archiver = createArchiver(src, dest);
        archiver.createArchive();

        File manifestFile = new File(dest, "child-1/META-INF/MANIFEST.MF");
        assertThat(manifestFile).content().hasLineCount(6);

        // change content of one file
        overwriteFileContent(manifestFile.toPath());
        assertThat(manifestFile).content().hasLineCount(1);

        archiver = createArchiver(src, dest);
        archiver.setForced(true);
        archiver.createArchive();

        // content was changed
        assertThat(manifestFile).content().hasLineCount(6);
    }

    private Archiver createArchiver(File src, File dest) {
        assertThat(src).isFile();
        DefaultArchivedFileSet afs = DefaultArchivedFileSet.archivedFileSet(src);
        afs.setIncludes(DEFAULT_INCLUDES_ARRAY);
        afs.setExcludes(null);
        afs.setPrefix("child-1/");
        afs.setStreamTransformer(new IdentityTransformer());
        Archiver archiver = lookup(Archiver.class, "dir");
        archiver.setDestFile(dest);
        archiver.addArchivedFileSet(afs, StandardCharsets.UTF_8);
        return archiver;
    }

    private void overwriteFileContent(Path path) throws IOException {
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);

        try (BufferedWriter writer =
                Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("TEST123");
        }

        Files.setLastModifiedTime(path, lastModifiedTime);
    }
}
