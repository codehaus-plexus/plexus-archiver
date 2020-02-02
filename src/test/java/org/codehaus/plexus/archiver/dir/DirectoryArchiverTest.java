package org.codehaus.plexus.archiver.dir;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class DirectoryArchiverTest {

    private static final File CURRENT_DIR = new File(".");
    private static final File DEST_DIR = new File("target/output/testCreateDirsForSymlink");
    private static final File SYMLINK_FILE = new File(DEST_DIR, "testSymlink");

    @Before
    public void prepare()
    {
        SYMLINK_FILE.delete();
        DEST_DIR.delete();
    }

    @After
    public void cleanUp()
    {
        prepare();
    }

    /**
     * Test case for ISSUE-130
     * @throws IOException
     */
    @Test
    public void testSymlinkWithParentDirectory()
        throws IOException
    {
        DirectoryArchiver archiver = new DirectoryArchiver();
        archiver.addSymlink(SYMLINK_FILE.getPath(), CURRENT_DIR.getPath());
        archiver.setDestFile(CURRENT_DIR);
        archiver.execute();
        assertTrue(SYMLINK_FILE.isDirectory());
    }
}
