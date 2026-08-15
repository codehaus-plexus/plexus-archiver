package org.codehaus.plexus.archiver;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test support class.
 */
public abstract class TestSupport extends InjectedTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static Path basedir() {
        return Path.of(System.getProperty("basedir", (new File("")).getAbsolutePath()));
    }

    protected static File getTestFile(final String path) {
        return basedir().resolve(path).toFile();
    }

    protected static File getTestFile(final String basedir, final String path) {
        return basedir().resolve(basedir).resolve(path).toFile();
    }
}
