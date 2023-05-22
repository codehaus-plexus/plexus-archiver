package org.codehaus.plexus.archiver.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.codehaus.plexus.components.io.attributes.FileAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchiveEntryUtilsTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testChmodForFileWithDollarPLXCOMP164() throws Exception {
        File temp = Files.createTempFile(tempDir, "A$A", "BB$").toFile();
        ArchiveEntryUtils.chmod(temp, 0770);
        assert0770(temp);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testChmodWithJava7() throws Exception {
        File temp = Files.createTempFile(tempDir, "D$D", "BB$").toFile();
        ArchiveEntryUtils.chmod(temp, 0770);
        assert0770(temp);
    }

    private void assert0770(File temp) throws IOException {
        FileAttributes j7 = new FileAttributes(temp, new HashMap<Integer, String>(), new HashMap<Integer, String>());
        assertTrue(j7.isGroupExecutable());
        assertTrue(j7.isGroupReadable());
        assertTrue(j7.isGroupWritable());

        assertFalse(j7.isWorldExecutable());
        assertFalse(j7.isWorldReadable());
        assertFalse(j7.isWorldWritable());
    }
}
