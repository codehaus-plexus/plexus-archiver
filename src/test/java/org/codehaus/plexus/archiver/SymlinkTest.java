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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkOverwriteZip() throws Exception {
        // Create temporary directory structure for testing
        File tempDir = new File("target/test-symlink-overwrite");
        // Clean up from any previous test runs
        if (tempDir.exists()) {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(tempDir);
        }
        tempDir.mkdirs();

        // Create two target files
        File target1 = new File(tempDir, "target1.txt");
        File target2 = new File(tempDir, "target2.txt");
        Files.write(target1.toPath(), "content1".getBytes());
        Files.write(target2.toPath(), "content2".getBytes());

        // Create first archive with symlink pointing to target1
        File archive1Dir = new File(tempDir, "archive1");
        archive1Dir.mkdirs();
        File archive1Target1 = new File(archive1Dir, "target1.txt");
        Files.write(archive1Target1.toPath(), "content1".getBytes());
        Files.createSymbolicLink(
                new File(archive1Dir, "link.txt").toPath(),
                archive1Target1.toPath().getFileName());

        ZipArchiver archiver1 = (ZipArchiver) lookup(Archiver.class, "zip");
        archiver1.addDirectory(archive1Dir);
        File zipFile1 = new File(tempDir, "archive1.zip");
        archiver1.setDestFile(zipFile1);
        archiver1.createArchive();

        // Extract first archive
        File outputDir = new File(tempDir, "output");
        outputDir.mkdirs();
        ZipUnArchiver unarchiver1 = (ZipUnArchiver) lookup(UnArchiver.class, "zip");
        unarchiver1.setSourceFile(zipFile1);
        unarchiver1.setDestFile(outputDir);
        unarchiver1.extract();

        // Verify symlink points to target1.txt
        File extractedLink = new File(outputDir, "link.txt");
        assertTrue(Files.isSymbolicLink(extractedLink.toPath()));
        assertEquals(
                "target1.txt", Files.readSymbolicLink(extractedLink.toPath()).toString());

        // Create second archive with symlink pointing to target2
        File archive2Dir = new File(tempDir, "archive2");
        archive2Dir.mkdirs();
        File archive2Target2 = new File(archive2Dir, "target2.txt");
        Files.write(archive2Target2.toPath(), "content2".getBytes());
        Files.createSymbolicLink(
                new File(archive2Dir, "link.txt").toPath(),
                archive2Target2.toPath().getFileName());

        ZipArchiver archiver2 = (ZipArchiver) lookup(Archiver.class, "zip");
        archiver2.addDirectory(archive2Dir);
        File zipFile2 = new File(tempDir, "archive2.zip");
        archiver2.setDestFile(zipFile2);
        archiver2.createArchive();

        // Extract second archive (should overwrite the symlink)
        ZipUnArchiver unarchiver2 = (ZipUnArchiver) lookup(UnArchiver.class, "zip");
        unarchiver2.setSourceFile(zipFile2);
        unarchiver2.setDestFile(outputDir);
        unarchiver2.extract();

        // Verify symlink now points to target2.txt (THIS IS THE KEY TEST)
        assertTrue(Files.isSymbolicLink(extractedLink.toPath()), "link.txt should still be a symlink");
        assertEquals(
                "target2.txt",
                Files.readSymbolicLink(extractedLink.toPath()).toString(),
                "Symlink should be updated to point to target2.txt");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSymlinkOverwriteTar() throws Exception {
        // Create temporary directory structure for testing
        File tempDir = new File("target/test-symlink-overwrite-tar");
        // Clean up from any previous test runs
        if (tempDir.exists()) {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(tempDir);
        }
        tempDir.mkdirs();

        // Create two target files
        File target1 = new File(tempDir, "target1.txt");
        File target2 = new File(tempDir, "target2.txt");
        Files.write(target1.toPath(), "content1".getBytes());
        Files.write(target2.toPath(), "content2".getBytes());

        // Create first archive with symlink pointing to target1
        File archive1Dir = new File(tempDir, "archive1");
        archive1Dir.mkdirs();
        File archive1Target1 = new File(archive1Dir, "target1.txt");
        Files.write(archive1Target1.toPath(), "content1".getBytes());
        Files.createSymbolicLink(
                new File(archive1Dir, "link.txt").toPath(),
                archive1Target1.toPath().getFileName());

        TarArchiver archiver1 = (TarArchiver) lookup(Archiver.class, "tar");
        archiver1.setLongfile(TarLongFileMode.posix);
        archiver1.addDirectory(archive1Dir);
        File tarFile1 = new File(tempDir, "archive1.tar");
        archiver1.setDestFile(tarFile1);
        archiver1.createArchive();

        // Extract first archive
        File outputDir = new File(tempDir, "output");
        outputDir.mkdirs();
        TarUnArchiver unarchiver1 = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        unarchiver1.setSourceFile(tarFile1);
        unarchiver1.setDestFile(outputDir);
        unarchiver1.extract();

        // Verify symlink points to target1.txt
        File extractedLink = new File(outputDir, "link.txt");
        assertTrue(Files.isSymbolicLink(extractedLink.toPath()));
        assertEquals(
                "target1.txt", Files.readSymbolicLink(extractedLink.toPath()).toString());

        // Create second archive with symlink pointing to target2
        File archive2Dir = new File(tempDir, "archive2");
        archive2Dir.mkdirs();
        File archive2Target2 = new File(archive2Dir, "target2.txt");
        Files.write(archive2Target2.toPath(), "content2".getBytes());
        Files.createSymbolicLink(
                new File(archive2Dir, "link.txt").toPath(),
                archive2Target2.toPath().getFileName());

        TarArchiver archiver2 = (TarArchiver) lookup(Archiver.class, "tar");
        archiver2.setLongfile(TarLongFileMode.posix);
        archiver2.addDirectory(archive2Dir);
        File tarFile2 = new File(tempDir, "archive2.tar");
        archiver2.setDestFile(tarFile2);
        archiver2.createArchive();

        // Extract second archive (should overwrite the symlink)
        TarUnArchiver unarchiver2 = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        unarchiver2.setSourceFile(tarFile2);
        unarchiver2.setDestFile(outputDir);
        unarchiver2.extract();

        // Verify symlink now points to target2.txt (THIS IS THE KEY TEST)
        assertTrue(Files.isSymbolicLink(extractedLink.toPath()), "link.txt should still be a symlink");
        assertEquals(
                "target2.txt",
                Files.readSymbolicLink(extractedLink.toPath()).toString(),
                "Symlink should be updated to point to target2.txt");
    }
}
