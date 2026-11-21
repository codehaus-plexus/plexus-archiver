/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.tar;

import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.Streams;
import org.codehaus.plexus.components.io.filemappers.FileMapper;

import static org.codehaus.plexus.archiver.util.Streams.bufferedInputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileInputStream;

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
@Named("tar")
public class TarUnArchiver extends AbstractUnArchiver {

    public TarUnArchiver() {}

    public TarUnArchiver(File sourceFile) {
        super(sourceFile);
    }

    /**
     * compression method
     */
    private UntarCompressionMethod compression = UntarCompressionMethod.NONE;

    private boolean failOnSymlinkTraversal;

    /**
     * Controls rejection of intermediate symbolic links in selected, mapped extraction paths.
     * The default is {@code false}, allowing contained directory symlinks as GNU tar does.
     * When enabled, destinations are checked before overwrite decisions or filesystem changes.
     * An offending entry throws {@link ArchiverException}, leaving earlier extracted entries in place.
     * The configured destination directory is trusted, and symbolic-link entries remain allowed.
     * Existing destination-containment checks apply with either setting.
     *
     * @param failOnSymlinkTraversal whether intermediate symbolic links cause extraction to fail
     * @since 5.0.0
     */
    public void setFailOnSymlinkTraversal(boolean failOnSymlinkTraversal) {
        this.failOnSymlinkTraversal = failOnSymlinkTraversal;
    }

    /**
     * Returns whether intermediate symbolic links cause TAR extraction to fail.
     *
     * @return {@code true} when symlink traversal is rejected; {@code false} by default
     * @since 5.0.0
     */
    public boolean isFailOnSymlinkTraversal() {
        return failOnSymlinkTraversal;
    }

    /**
     * Set decompression algorithm to use; default=none.
     * <p>
     * Allowable values are </p>
     * <ul>
     * <li>none - no compression</li>
     * <li>gzip - Gzip compression</li>
     * <li>bzip2 - Bzip2 compression</li>
     * <li>snappy - Snappy compression</li>
     * <li>xz - Xz compression</li>
     * </ul>
     *
     * @param method compression method
     */
    public void setCompression(UntarCompressionMethod method) {
        compression = method;
    }

    /**
     * No encoding support in Untar.
     */
    public void setEncoding(String encoding) {
        getLogger().warn("The TarUnArchiver doesn't support the encoding attribute");
    }

    @Override
    protected void execute() throws ArchiverException {
        execute(getSourceFile(), getDestDirectory(), getFileMappers());
    }

    @Override
    protected void execute(String path, File outputDirectory) {
        execute(new File(path), getDestDirectory(), getFileMappers());
    }

    protected void execute(File sourceFile, File destDirectory, FileMapper[] fileMappers) throws ArchiverException {
        try {
            getLogger().info("Expanding: " + sourceFile + " into " + destDirectory);
            TarFile tarFile = new TarFile(sourceFile);
            try (TarArchiveInputStream tis = new TarArchiveInputStream(
                    decompress(compression, sourceFile, bufferedInputStream(fileInputStream(sourceFile))))) {
                TarArchiveEntry te;
                while ((te = tis.getNextEntry()) != null) {
                    TarResource fileInfo = new TarResource(tarFile, te);
                    if (isSelected(te.getName(), fileInfo)) {
                        final String symlinkDestination = te.isSymbolicLink() ? te.getLinkName() : null;
                        extractFile(
                                sourceFile,
                                destDirectory,
                                tis,
                                te.getName(),
                                te.getModTime(),
                                te.isDirectory(),
                                te.getMode() != 0 ? te.getMode() : null,
                                symlinkDestination,
                                fileMappers);
                    }
                }
                getLogger().debug("expand complete");
            }
        } catch (IOException ioe) {
            throw new ArchiverException("Error while expanding " + sourceFile.getAbsolutePath(), ioe);
        }
    }

    /**
     * Checks mapped destinations while preserving the existing extraction-hook signature.
     *
     * @param source the source archive
     * @param directory the extraction directory
     * @param contents the entry contents
     * @param name the original archive member name
     * @param date the entry timestamp
     * @param isDirectory whether the entry is a directory
     * @param mode the entry permissions, or null
     * @param symlink the symbolic-link target, or null
     * @param fileMappers the configured file mappers
     * @throws IOException if mapping checks or extraction fail
     * @throws ArchiverException if the destination is rejected
     */
    @Override
    protected void extractFile(
            File source,
            File directory,
            InputStream contents,
            String name,
            Date date,
            boolean isDirectory,
            Integer mode,
            String symlink,
            FileMapper[] fileMappers)
            throws IOException {
        String destination = name;
        if (fileMappers != null) {
            for (FileMapper mapper : fileMappers) {
                destination = mapper.getMappedFileName(destination);
            }
        }
        checkSymlinkTraversal(directory, destination, name);
        super.extractFile(source, directory, contents, destination, date, isDirectory, mode, symlink, null);
    }

    /**
     * Checks existing parent components without following links, including links preceding a later {@code ..}.
     * Missing parents are allowed because extraction creates them after validation.
     */
    private void checkSymlinkTraversal(File directory, String name, String entryName) throws IOException {
        if (!failOnSymlinkTraversal) {
            return;
        }
        Path root = directory.toPath().toAbsolutePath();
        Path canonicalRoot = resolveExtractionRoot(directory);
        // Extraction accepts either separator in absolute names, but keeps relative names platform-native.
        Path portable = Path.of(name.replace('/', File.separatorChar).replace('\\', File.separatorChar));
        // Resolve the trusted directory first: a symlink followed by '..' can change which tree owns its children.
        Path path = portable.isAbsolute() ? portable : canonicalRoot.resolve(Path.of(name));
        int suffixStart = portable.isAbsolute() ? trustedRootPrefixLength(portable, root) : -1;
        if (suffixStart >= 0) {
            // Preserve the untrusted suffix without relativize(), which would collapse its parent components.
            path = canonicalRoot;
            for (int i = suffixStart; i < portable.getNameCount(); i++) {
                path = path.resolve(portable.getName(i));
            }
        }
        Path component = path.getRoot();
        boolean reachedRoot = canonicalRoot.equals(component);
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            // Normalize one component at a time: any preceding symlink was checked before a parent can erase it.
            component = component.resolve(path.getName(i)).normalize();
            // A later '..' must not restore ancestor exemptions for the untrusted suffix.
            reachedRoot |= canonicalRoot.equals(component);
            // Only the actual trusted root and its ancestors are exempt from traversal checks.
            if (canonicalRoot.startsWith(component)) {
                continue;
            }
            try {
                if (Files.readAttributes(component, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                        .isSymbolicLink()) {
                    if (!reachedRoot) {
                        Path resolved = resolveExtractionRoot(component.toFile());
                        // Accept ancestor aliases such as macOS /var, but not a separate alias of the root itself.
                        // Resolve only this prefix so symlinks and parent components in the suffix remain visible.
                        if (!resolved.equals(canonicalRoot) && canonicalRoot.startsWith(resolved)) {
                            component = resolved;
                            continue;
                        }
                    }
                    throw new ArchiverException("Cannot extract TAR entry '" + entryName + "': "
                            + "destination '" + name
                            + "' traverses symbolic link '" + component + "'");
                }
            } catch (NoSuchFileException ignored) {
                // Continue so a later parent component cannot hide a different, existing symlink.
            }
        }
    }

    /**
     * Matches an absolute configured root while ignoring harmless dot components in either spelling.
     * Parent components are compared literally so symlinks preceding {@code ..} retain their meaning.
     *
     * @param path the absolute mapped path whose suffix must remain unchanged
     * @param root the absolute configured destination before filesystem resolution
     * @return the index of the first suffix component, or {@code -1} when the trusted prefix does not match
     */
    private static int trustedRootPrefixLength(Path path, Path root) {
        if (!root.getRoot().equals(path.getRoot())) {
            return -1;
        }
        int next = 0;
        for (Path component : root) {
            if (component.toString().equals(".")) {
                continue;
            }
            // Skip dots only while matching the trusted prefix; preserve every component of the remaining suffix.
            while (next < path.getNameCount() && path.getName(next).toString().equals(".")) {
                next++;
            }
            if (next == path.getNameCount() || !component.equals(path.getName(next))) {
                return -1;
            }
            next++;
        }
        return next;
    }

    /**
     * This method wraps the input stream with the
     * corresponding decompression method
     *
     * @param file provides location information for BuildException
     * @param istream input stream
     *
     * @return input stream with on-the-fly decompression
     *
     * @throws IOException thrown by GZIPInputStream constructor
     */
    private InputStream decompress(UntarCompressionMethod compression, final File file, final InputStream istream)
            throws IOException, ArchiverException {
        if (compression == UntarCompressionMethod.GZIP) {
            return Streams.bufferedInputStream(new GZIPInputStream(istream));
        } else if (compression == UntarCompressionMethod.BZIP2) {
            return new BZip2CompressorInputStream(istream);
        } else if (compression == UntarCompressionMethod.SNAPPY) {
            return new FramedSnappyCompressorInputStream(istream);
        } else if (compression == UntarCompressionMethod.XZ) {
            return new XZCompressorInputStream(istream);
        } else if (compression == UntarCompressionMethod.ZSTD) {
            return new ZstdCompressorInputStream(istream);
        }
        return istream;
    }

    /**
     * Valid Modes for Compression attribute to Untar Task
     */
    public enum UntarCompressionMethod {
        NONE("none"),
        GZIP("gzip"),
        BZIP2("bzip2"),
        SNAPPY("snappy"),
        XZ("xz"),
        ZSTD("zstd");

        final String value;

        /**
         * Constructor
         */
        UntarCompressionMethod(String value) {
            this.value = value;
        }
    }
}
