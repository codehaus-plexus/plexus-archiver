package org.codehaus.plexus.archiver;

import java.io.File;
import java.nio.file.Files;

import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class HardlinkTest extends TestSupport {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testHardlinkTar() throws Exception {
        // Extract test files
        final File archiveFile = getTestFile("src/test/resources/hardlinks/hardlinks.tar");
        File output = getTestFile("target/output/untaredHardlinks");
        output.mkdirs();
        TarUnArchiver unarchiver = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        unarchiver.setSourceFile(archiveFile);
        unarchiver.setDestFile(output);
        unarchiver.extract();
        // Check that we have hardlinks
        assertTrue(Files.isSameFile(
                output.toPath().resolve("fileR.txt"), output.toPath().resolve("hardlink")));

        // Archive the extracted hardlinks to new archive
        TarArchiver archiver = (TarArchiver) lookup(Archiver.class, "tar");
        archiver.setLongfile(TarLongFileMode.posix);
        archiver.addDirectory(output);
        final File testFile = getTestFile("target/output/untaredHardlinks2.tar");
        archiver.setDestFile(testFile);
        archiver.createArchive();

        // Check that our created archive actually contains hardlinks when extracted
        unarchiver = (TarUnArchiver) lookup(UnArchiver.class, "tar");
        output = getTestFile("target/output/untaredHardlinks2");
        output.mkdirs();
        unarchiver.setSourceFile(testFile);
        unarchiver.setDestFile(output);
        unarchiver.extract();
        assertTrue(Files.isSameFile(
                output.toPath().resolve("fileR.txt"), output.toPath().resolve("hardlink")));
    }
}
