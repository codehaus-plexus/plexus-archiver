package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractArchiverTest {

    private AbstractArchiver archiver;

    @BeforeEach
    void setUp() throws Exception {
        this.archiver = new AbstractArchiver() {

            @Override
            protected String getArchiveType() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void execute() throws ArchiverException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void close() throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    void testModesAndOverridesAreUnsetByDefault() {
        assertEquals(-1, archiver.getDefaultFileMode());
        assertEquals(-1, archiver.getOverrideFileMode());

        assertEquals(Archiver.DEFAULT_DIR_MODE, archiver.getDefaultDirectoryMode());
        assertEquals(-1, archiver.getOverrideDirectoryMode());
    }

    @Test
    void testWhenUnsetModeUsesDefault() {
        assertEquals(Archiver.DEFAULT_FILE_MODE, archiver.getFileMode());
        assertEquals(Archiver.DEFAULT_DIR_MODE, archiver.getDirectoryMode());
    }

    @Test
    void testSetModeIsUsedWithFlagsForType() {
        archiver.setFileMode(0400);
        assertEquals(0100400, archiver.getFileMode());

        archiver.setDirectoryMode(0600);
        assertEquals(040600, archiver.getDirectoryMode());
    }

    @Test
    void testSetDefaultIncludesFlagsForType() {
        archiver.setDefaultFileMode(0400);
        assertEquals(0100400, archiver.getDefaultFileMode());

        archiver.setDefaultDirectoryMode(0600);
        assertEquals(040600, archiver.getDefaultDirectoryMode());
    }

    @Test
    void testDefaultIsUsedWhenModeIsUnset() {
        archiver.setDefaultFileMode(0400);
        assertEquals(0100400, archiver.getFileMode());

        archiver.setDefaultDirectoryMode(0600);
        assertEquals(040600, archiver.getDirectoryMode());
    }

    @Test
    void testOverridesCanBeReset() {
        archiver.setFileMode(0400);
        archiver.setFileMode(-1);
        assertEquals(-1, archiver.getOverrideFileMode());

        archiver.setDirectoryMode(0600);
        archiver.setDirectoryMode(-1);
        assertEquals(-1, archiver.getOverrideDirectoryMode());
    }

    @Test
    void testSetDestFileInTheWorkingDir() {
        archiver.setDestFile(new File("archive"));
    }
}
