package org.codehaus.plexus.archiver;

import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.plexus.archiver.tar.PlexusIoTarFileResourceCollection;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.components.io.resources.ResourceFactory;
import org.codehaus.plexus.components.io.resources.proxy.PlexusIoProxyResourceCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractArchiverTest {

    @TempDir
    Path temp;

    private AbstractArchiver archiver;

    private final Deque<PlexusIoResourceCollection> internallyCreated = new ArrayDeque<>();

    private int resourceRegistrations;

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

            @Override
            protected PlexusIoResourceCollection asResourceCollection(ArchivedFileSet fileSet, Charset charset) {
                return internallyCreated.removeFirst();
            }

            @Override
            public void addResources(PlexusIoResourceCollection collection) {
                resourceRegistrations++;
                super.addResources(collection);
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
        internallyCreated.add(new PlexusIoProxyResourceCollection(new PlexusIoProxyResourceCollection(first)));
        internallyCreated.add(second);
        archiver.addArchivedFileSet(new DefaultArchivedFileSet(new File("first.tar")));
        archiver.addArchivedFileSet(new DefaultArchivedFileSet(new File("second.tar")), StandardCharsets.UTF_8);
        assertEquals(2, resourceRegistrations, "Internal collection registration must retain the public subclass hook");
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

    /** Caller-owned collections survive archive success and failure, while their consumed handles are closed. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void callerSuppliedCollectionsRemainOpen(boolean failure) throws Exception {
        AtomicInteger iteratorCloses = new AtomicInteger();
        AtomicInteger streamCloses = new AtomicInteger();
        AtomicInteger collectionCloses = new AtomicInteger();
        Path file = Files.writeString(temp.resolve("file"), "payload");
        PlexusIoFileResource original = (PlexusIoFileResource) ResourceFactory.createResource(file.toFile());
        PlexusIoResource resource = new PlexusIoFileResource(file.toFile(), "file", original.getAttributes()) {
            @Override
            public InputStream getContents() throws IOException {
                return new FilterInputStream(Files.newInputStream(file)) {
                    @Override
                    public void close() throws IOException {
                        streamCloses.incrementAndGet();
                        super.close();
                    }
                };
            }
        };
        class ResourceIterator implements Iterator<PlexusIoResource>, Closeable {
            private boolean consumed;

            @Override
            public boolean hasNext() {
                if (failure && consumed) {
                    throw new IllegalStateException("iteration failed");
                }
                return !consumed;
            }

            @Override
            public PlexusIoResource next() {
                if (consumed) {
                    throw new NoSuchElementException();
                }
                consumed = true;
                return resource;
            }

            @Override
            public void close() {
                iteratorCloses.incrementAndGet();
            }
        }
        PlexusIoTarFileResourceCollection collection = new PlexusIoTarFileResourceCollection() {
            @Override
            public Iterator<PlexusIoResource> getResources() {
                return new ResourceIterator();
            }

            @Override
            public void close() {
                collectionCloses.incrementAndGet();
            }
        };
        TarArchiver consumer = new TarArchiver();
        consumer.setDestFile(temp.resolve("written.tar").toFile());
        consumer.addResources(new PlexusIoProxyResourceCollection(new PlexusIoProxyResourceCollection(collection)));
        if (failure) {
            assertThrows(IllegalStateException.class, consumer::createArchive);
        } else {
            consumer.createArchive();
        }
        assertTrue(streamCloses.get() > 0);
        assertEquals(1, iteratorCloses.get());
        assertEquals(0, collectionCloses.get());
        collection.close();
        assertEquals(1, collectionCloses.get());
    }
}
