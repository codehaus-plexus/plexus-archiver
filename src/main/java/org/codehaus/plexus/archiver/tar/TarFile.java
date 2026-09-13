package org.codehaus.plexus.archiver.tar;

import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.codehaus.plexus.archiver.ArchiveFile;
import org.codehaus.plexus.archiver.util.Streams;

import static org.codehaus.plexus.archiver.util.Streams.bufferedInputStream;

/**
 * Implementation of {@link ArchiveFile} for TAR files, allowing consumers to use
 * the same archive abstraction for TAR and ZIP files.
 *
 * <p>Unlike {@link org.apache.commons.compress.archivers.zip.ZipFile}, TAR has no
 * catalog for direct entry lookup. Headers are recorded as they are encountered;
 * opening an enumeration does not scan the archive. For efficient access, iterate
 * over {@link #getEntries()} and consume each current member's contents in archive
 * order, as with {@link TarArchiveInputStream}. This reads the source once.
 *
 * <p>Explicitly requesting an earlier or already-consumed member's contents may
 * reopen the archive and scan to that member, including decompression for compressed
 * sources. These on-demand reads use a separate cursor and do not skip enumeration
 * entries. Finish reading and close each content stream before requesting another;
 * concurrent content streams and simultaneous enumerations are not supported.
 *
 * <p>Hard links must refer to earlier archive members. Their logical streams expose
 * the data-bearing member's bytes, which are cached on demand once per occurrence.
 * Merely enumerating a hard link or inspecting its metadata does not read its payload.
 * Close this reader and its content streams, preferably using try-with-resources,
 * to release handles and delete cached payloads. Closing a content stream leaves
 * the reader open.
 */
public class TarFile implements ArchiveFile, Closeable {
    private final File file;
    private Cursor enumerationCursor;
    private Cursor replayCursor;
    private TarEntryIndex index = new TarEntryIndex();
    private TarPayloads linkPayloads;

    /** Creates an archive reader; streams are opened lazily. */
    public TarFile(File file) {
        this.file = file;
    }

    /**
     * Lazily enumerates headers in archive order, without a preliminary scan.
     * Pass returned headers to {@link #getInputStream(TarArchiveEntry)} to identify
     * exact occurrences when names repeat. Content lookups do not advance enumeration.
     *
     * @return the archive headers in their original order
     * @throws IOException if the source cannot be opened
     */
    @Override
    public Enumeration<ArchiveEntry> getEntries() throws IOException {
        if (enumerationCursor != null) {
            enumerationCursor.close();
        }
        Cursor cursor = enumerationCursor = new Cursor();
        return new Enumeration<ArchiveEntry>() {
            private TarArchiveEntry next;
            private boolean ready;
            private boolean ended;

            /** Reads at most one new header, so repeated checks cannot skip a member. */
            @Override
            public boolean hasMoreElements() {
                if (!ready && !ended) {
                    try {
                        next = cursor.next();
                        ready = next != null;
                        ended = !ready;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return ready;
            }

            /** Returns the pending header while leaving its payload ready for a sequential read. */
            @Override
            public ArchiveEntry nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                ready = false;
                return next;
            }
        };
    }

    /** Returns metadata already encountered by enumeration or an explicit lookup. */
    TarEntryIndex index() {
        return index;
    }

    /** Finds a caller-created header on demand without moving the enumeration cursor. */
    private TarArchiveEntry findEntry(TarArchiveEntry entry) throws IOException {
        Integer position = index.find(entry);
        if (position != null) {
            return index.entry(position);
        }
        if (replayCursor == null) {
            replayCursor = new Cursor();
        }
        TarArchiveEntry next;
        while ((next = replayCursor.next()) != null) {
            if (TarEntryIndex.normalizedName(next.getName()).equals(TarEntryIndex.normalizedName(entry.getName()))) {
                return next;
            }
        }
        throw new IOException("Unknown TAR entry: " + entry.getName());
    }

    /** Resolves backward links using recorded headers, with no payload reads. */
    TarArchiveEntry resolve(TarArchiveEntry entry) throws IOException {
        return index.resolve(findEntry(entry));
    }

    /** Returns logical contents while preserving occurrence identity in TAR headers. */
    @Override
    public InputStream getInputStream(ArchiveEntry entry) throws IOException {
        return getInputStream(
                entry instanceof TarArchiveEntry ? (TarArchiveEntry) entry : new TarArchiveEntry(entry.getName()));
    }

    /**
     * Returns a member's logical contents; a caller-created header selects the first
     * matching name. Reading the current ordinary member streams directly, whereas
     * earlier or already-consumed contents may require an independent backward read.
     * Hard-link payloads are cached once per data-bearing occurrence.
     * Close this stream before requesting another; closing it does not close the reader.
     *
     * @param entry the member whose logical contents are requested
     * @return the member's contents, resolving valid backward hard links
     * @throws IOException if reading fails or the link is invalid
     */
    public InputStream getInputStream(TarArchiveEntry entry) throws IOException {
        TarArchiveEntry actual = findEntry(entry);
        if (actual.isLink()) {
            TarArchiveEntry target = index.resolve(actual);
            if (linkPayloads == null) {
                linkPayloads = new TarPayloads();
            }
            return java.nio.file.Files.newInputStream(linkPayloads.add(this, target));
        }
        return rawContents(actual);
    }

    /** Uses the live payload when available, reserving a separate cursor for explicit replay. */
    InputStream rawContents(TarArchiveEntry entry) throws IOException {
        if (enumerationCursor != null && enumerationCursor.available(entry)) {
            return enumerationCursor.contents();
        }
        int position = index.position(entry);
        if (replayCursor != null && replayCursor.available(entry)) {
            return replayCursor.contents();
        }
        if (replayCursor == null || replayCursor.position >= position) {
            if (replayCursor != null) {
                replayCursor.close();
            }
            replayCursor = new Cursor();
        }
        while (replayCursor.position < position) {
            if (replayCursor.next() == null) {
                throw new IOException("TAR changed while reading " + entry.getName());
            }
        }
        return replayCursor.contents();
    }

    /** Opens the source; compressed subclasses supply their decompression stream here. */
    protected InputStream getInputStream(File file) throws IOException {
        return Streams.fileInputStream(file);
    }

    /** Owns one sequential decoder and records headers without consuming their payloads early. */
    private final class Cursor implements Closeable {
        private final TarArchiveInputStream input;
        private int position = -1;
        private TarArchiveEntry entry;
        private boolean claimed;

        /** Opens a decoder only when enumeration or a content lookup actually needs it. */
        private Cursor() throws IOException {
            input = new TarArchiveInputStream(bufferedInputStream(getInputStream(file)), "UTF8");
        }

        /** Advances one header and reuses its canonical occurrence record across replay cursors. */
        private TarArchiveEntry next() throws IOException {
            TarArchiveEntry next = input.getNextEntry();
            entry = next == null ? null : index.record(++position, next);
            claimed = false;
            return entry;
        }

        /** Determines whether the current payload has not yet been handed to a consumer. */
        private boolean available(TarArchiveEntry requested) {
            return entry == requested && !claimed;
        }

        /** Gives the caller a non-owning stream while preserving the cursor for the next header. */
        private InputStream contents() {
            claimed = true;
            return new FilterInputStream(input) {
                /** Closing a member must not close the decoder used by later members. */
                @Override
                public void close() {}
            };
        }

        /** Releases this cursor's decoder and source handle. */
        @Override
        public void close() throws IOException {
            input.close();
        }
    }

    /** Releases both cursors and cached payloads, including when any individual close fails. */
    @Override
    public void close() throws IOException {
        try (Cursor enumeration = enumerationCursor;
                Cursor replay = replayCursor;
                TarPayloads payloads = linkPayloads) {
            // Resource ownership is independent, so every close must be attempted.
        } finally {
            enumerationCursor = null;
            replayCursor = null;
            linkPayloads = null;
            index = new TarEntryIndex();
        }
    }
}
