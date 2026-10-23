package org.codehaus.plexus.archiver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.plexus.archiver.tar.PlexusIoTarFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.proxy.PlexusIoProxyResourceCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void modesAndOverridesAreUnsetByDefault() {
        assertEquals(-1, archiver.getDefaultFileMode());
        assertEquals(-1, archiver.getOverrideFileMode());

        assertEquals(Archiver.DEFAULT_DIR_MODE, archiver.getDefaultDirectoryMode());
        assertEquals(-1, archiver.getOverrideDirectoryMode());
    }

    @Test
    void whenUnsetModeUsesDefault() {
        assertEquals(Archiver.DEFAULT_FILE_MODE, archiver.getFileMode());
        assertEquals(Archiver.DEFAULT_DIR_MODE, archiver.getDirectoryMode());
    }

    @Test
    void setModeIsUsedWithFlagsForType() {
        archiver.setFileMode(0400);
        assertEquals(0100400, archiver.getFileMode());

        archiver.setDirectoryMode(0600);
        assertEquals(040600, archiver.getDirectoryMode());
    }

    @Test
    void setDefaultIncludesFlagsForType() {
        archiver.setDefaultFileMode(0400);
        assertEquals(0100400, archiver.getDefaultFileMode());

        archiver.setDefaultDirectoryMode(0600);
        assertEquals(040600, archiver.getDefaultDirectoryMode());
    }

    @Test
    void defaultIsUsedWhenModeIsUnset() {
        archiver.setDefaultFileMode(0400);
        assertEquals(0100400, archiver.getFileMode());

        archiver.setDefaultDirectoryMode(0600);
        assertEquals(040600, archiver.getDirectoryMode());
    }

    @Test
    void overridesCanBeReset() {
        archiver.setFileMode(0400);
        archiver.setFileMode(-1);
        assertEquals(-1, archiver.getOverrideFileMode());

        archiver.setDirectoryMode(0600);
        archiver.setDirectoryMode(-1);
        assertEquals(-1, archiver.getOverrideDirectoryMode());
    }

    @Test
    void setDestFileInTheWorkingDir() {
        archiver.setDestFile(new File("archive"));
    }

    /** Failed iteration or closure must not prevent wrapped collections from releasing their owned resources. */
    @Test
    void cleanupReleasesIteratorsAndNestedCollectionsAfterFailures() throws Exception {
        AtomicInteger iteratorCloses = new AtomicInteger();
        AtomicInteger collectionCloses = new AtomicInteger();
        class FailingIterator implements Iterator<PlexusIoResource>, Closeable {
            /** Fails before exhaustion to exercise ownership of partially consumed iterators. */
            @Override
            public boolean hasNext() {
                throw new IllegalStateException("iteration failed");
            }

            /** No resource can be returned after the simulated iteration failure. */
            @Override
            public PlexusIoResource next() {
                throw new NoSuchElementException();
            }

            /** Fails during close so collection cleanup must still run. */
            @Override
            public void close() throws IOException {
                iteratorCloses.incrementAndGet();
                throw new IOException("iterator close failed");
            }
        }
        PlexusIoTarFileResourceCollection first = new PlexusIoTarFileResourceCollection() {
            /** Supplies a closeable iterator whose lifetime starts before the first read. */
            @Override
            public Iterator<PlexusIoResource> getResources() {
                return new FailingIterator();
            }

            /** A collection close failure must not skip the next registered collection. */
            @Override
            public void close() throws IOException {
                collectionCloses.incrementAndGet();
                throw new IOException("collection close failed");
            }
        };
        PlexusIoTarFileResourceCollection second = new PlexusIoTarFileResourceCollection() {
            /** Records cleanup even though iteration never reached this collection. */
            @Override
            public void close() {
                collectionCloses.incrementAndGet();
            }
        };
        archiver.addResources(new PlexusIoProxyResourceCollection(new PlexusIoProxyResourceCollection(first)));
        archiver.addResources(second);
        assertThrows(IllegalStateException.class, () -> archiver.getResources().hasNext());
        IOException failure = assertThrows(IOException.class, archiver::cleanUp);
        assertEquals("iterator close failed", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("collection close failed", failure.getSuppressed()[0].getMessage());
        assertEquals(1, iteratorCloses.get());
        assertEquals(2, collectionCloses.get());
        // Cleanup consumes its ownership records even when closing fails, allowing subsequent archive operations.
        archiver.cleanUp();
        assertEquals(1, iteratorCloses.get());
        assertEquals(2, collectionCloses.get());
    }
}
