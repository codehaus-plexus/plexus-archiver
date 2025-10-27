package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kristian Rosenvold
 */
class SymlinkTest extends TestSupport {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkDir() throws IOException {
        File dummyContent = getTestFile("src/test/resources/symlinks/src/symDir");
        assertTrue(dummyContent.isDirectory());
        assertTrue(Files.isSymbolicLink(dummyContent.toPath()));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkDirWithSlash() throws IOException {
        File dummyContent = getTestFile("src/test/resources/symlinks/src/symDir/");
        assertTrue(dummyContent.isDirectory());
        assertTrue(Files.isSymbolicLink(dummyContent.toPath()));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkFile() {
        File dummyContent = getTestFile("src/test/resources/symlinks/src/symR");
        assertFalse(dummyContent.isDirectory());
        assertTrue(Files.isSymbolicLink(dummyContent.toPath()));
    }

    @Test
    void testSymlinkTar() throws Exception {
        TarArchiver archiver = (TarArchiver) lookup(Archiver.class, "tar");
        archiver.setLongfile(TarLongFileMode.posix);

        File dummyContent = getTestFile("src/test/resources/symlinks/src");
        archiver.addDirectory(dummyContent);
        final File archiveFile = new File("target/output/symlinks.tar");
        archiver.setDestFile(archiveFile);
        archiver.createArchive();
        File output = getTestFile("target/output/untaredSymlinks");
        output.mkdirs();
        TarUnArchiver unarchiver = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        unarchiver.setSourceFile(archiveFile);
        unarchiver.setDestFile(output);
        unarchiver.extract();
        // second unpacking should also work
        unarchiver.extract();
    }

    @Test
    void testSymlinkZip() throws Exception {
        ZipArchiver archiver = (ZipArchiver) lookup(Archiver.class, "zip");

        File dummyContent = getTestFile("src/test/resources/symlinks/src");
        archiver.addDirectory(dummyContent);
        final File archiveFile = new File("target/output/symlinks.zip");
        archiveFile.delete();
        archiver.setDestFile(archiveFile);
        archiver.createArchive();

        File output = getTestFile("target/output/unzippedSymlinks");
        output.mkdirs();
        ZipUnArchiver unarchiver = (ZipUnArchiver) lookup(UnArchiver.class, "zip");
        unarchiver.setSourceFile(archiveFile);
        unarchiver.setDestFile(output);
        unarchiver.extract();
        // second unpacking should also work
        unarchiver.extract();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkDirArchiver() throws Exception {
        DirectoryArchiver archiver = (DirectoryArchiver) lookup(Archiver.class, "dir");

        File dummyContent = getTestFile("src/test/resources/symlinks/src");
        archiver.addDirectory(dummyContent);
        final File archiveFile = new File("target/output/dirarchiver-symlink");
        archiveFile.mkdirs();
        archiver.setDestFile(archiveFile);
        archiver.addSymlink("target/output/dirarchiver-symlink/aNewDir/symlink", ".");

        archiver.createArchive();

        File symbolicLink = new File("target/output/dirarchiver-symlink/symR");
        assertTrue(Files.isSymbolicLink(symbolicLink.toPath()));

        symbolicLink = new File("target/output/dirarchiver-symlink/aDirWithALink/backOutsideToFileX");
        assertTrue(Files.isSymbolicLink(symbolicLink.toPath()));
    }

    /**
     * Test for the issue where symlinks were not properly resolved when extracting archives.
     * When a symlink is extracted and then the archive is extracted again, the symlink
     * should remain a symlink and not be resolved to its target.
     * This test verifies:
     * 1. Extracting a symlink creates the symlink correctly
     * 2. Re-extracting the same archive preserves the symlink
     * 3. Symlinks to non-existent files are handled correctly
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkExtractionTwice() throws Exception {
        // Test with tar
        TarArchiver tarArchiver = (TarArchiver) lookup(Archiver.class, "tar");
        tarArchiver.setLongfile(TarLongFileMode.posix);

        File srcDir = getTestFile("src/test/resources/symlinks/src");
        tarArchiver.addDirectory(srcDir);
        File tarFile = new File("target/output/symlink-twice-test.tar");
        tarArchiver.setDestFile(tarFile);
        tarArchiver.createArchive();

        File outputDir = new File("target/output/symlink-twice-test-tar");
        outputDir.mkdirs();

        TarUnArchiver tarUnarchiver = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        tarUnarchiver.setSourceFile(tarFile);
        tarUnarchiver.setDestFile(outputDir);

        // First extraction
        tarUnarchiver.extract();

        // Verify symlinks exist and are actually symlinks
        File symR = new File(outputDir, "symR");
        assertTrue(Files.isSymbolicLink(symR.toPath()), "symR should be a symlink");

        // Second extraction - this should not fail and should preserve symlinks
        tarUnarchiver.extract();

        // Verify symlinks still exist and are still symlinks
        assertTrue(Files.isSymbolicLink(symR.toPath()), "symR should still be a symlink after re-extraction");

        // Test with zip
        ZipArchiver zipArchiver = (ZipArchiver) lookup(Archiver.class, "zip");
        zipArchiver.addDirectory(srcDir);
        File zipFile = new File("target/output/symlink-twice-test.zip");
        zipFile.delete();
        zipArchiver.setDestFile(zipFile);
        zipArchiver.createArchive();

        File zipOutputDir = new File("target/output/symlink-twice-test-zip");
        zipOutputDir.mkdirs();

        ZipUnArchiver zipUnarchiver = (ZipUnArchiver) lookup(UnArchiver.class, "zip");
        zipUnarchiver.setSourceFile(zipFile);
        zipUnarchiver.setDestFile(zipOutputDir);

        // First extraction
        zipUnarchiver.extract();

        // Verify symlinks exist and are actually symlinks
        File symRZip = new File(zipOutputDir, "symR");
        assertTrue(Files.isSymbolicLink(symRZip.toPath()), "symR should be a symlink in zip");

        // Second extraction - this should not fail and should preserve symlinks
        zipUnarchiver.extract();

        // Verify symlinks still exist and are still symlinks
        assertTrue(Files.isSymbolicLink(symRZip.toPath()), "symR should still be a symlink after re-extraction in zip");
    }
}
