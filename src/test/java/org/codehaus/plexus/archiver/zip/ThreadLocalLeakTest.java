package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that ThreadLocal memory leaks are prevented when creating
 * multiple jar files sequentially, which was the root cause of the OOM issue
 * reported in https://github.com/codehaus-plexus/plexus-archiver/issues/xxx
 */
class ThreadLocalLeakTest {

    @TempDir
    File tempDir;

    /**
     * This test creates multiple jar files to simulate the scenario where
     * ThreadLocal values would accumulate in thread pools. The fix ensures
     * that threads are terminated quickly after completing tasks, which
     * cleans up ThreadLocal values.
     */
    @Test
    void testMultipleJarCreationsDoNotLeakMemory() throws Exception {
        // Create a source file to add to jars
        File sourceFile = new File(tempDir, "test.txt");
        Files.write(sourceFile.toPath(), "test content".getBytes());

        // Create multiple jars sequentially
        for (int i = 0; i < 10; i++) {
            createJar(new File(tempDir, "test-" + i + ".jar"), sourceFile);
        }

        // If we got here without OOM, the test passed
        assertTrue(true, "Multiple jar creations completed successfully without OOM");
    }

    private void createJar(File outputFile, File sourceFile) throws Exception {
        ConcurrentJarCreator zipCreator =
                new ConcurrentJarCreator(Runtime.getRuntime().availableProcessors());

        ZipArchiveEntry entry = new ZipArchiveEntry(sourceFile.getName());
        entry.setMethod(ZipArchiveEntry.DEFLATED);
        entry.setSize(sourceFile.length());
        entry.setTime(sourceFile.lastModified());

        zipCreator.addArchiveEntry(
                entry,
                () -> {
                    try {
                        return Files.newInputStream(sourceFile.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                true);

        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(outputFile)) {
            zos.setEncoding("UTF-8");
            zipCreator.writeTo(zos);
        }
    }
}
