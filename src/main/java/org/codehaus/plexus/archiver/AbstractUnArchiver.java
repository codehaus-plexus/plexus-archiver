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
package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.components.io.attributes.SymlinkUtils;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

// TODO there should really be constructors which take the source file.

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
public abstract class AbstractUnArchiver implements UnArchiver, FinalizerEnabled {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected Logger getLogger() {
        return logger;
    }

    private File destDirectory;

    private File destFile;

    private File sourceFile;

    private boolean overwrite = true;

    private FileMapper[] fileMappers;

    private List<ArchiveFinalizer> finalizers;

    /** Directory spellings are reused only after a fresh NOFOLLOW identity check, within one extraction. */
    private Map<Path, ResolvedDirectory> resolvedDirectories;

    private record ResolvedDirectory(Object fileKey, Path path) {}

    private FileSelector[] fileSelectors;

    /**
     * @since 1.1
     */
    private boolean ignorePermissions = false;

    public AbstractUnArchiver() {
        // no op
    }

    public AbstractUnArchiver(final File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public File getDestDirectory() {
        return destDirectory;
    }

    @Override
    public void setDestDirectory(final File destDirectory) {
        this.destDirectory = destDirectory;
    }

    @Override
    public File getDestFile() {
        return destFile;
    }

    @Override
    public void setDestFile(final File destFile) {
        this.destFile = destFile;
    }

    @Override
    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public void setSourceFile(final File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public boolean isOverwrite() {
        return overwrite;
    }

    @Override
    public void setOverwrite(final boolean b) {
        overwrite = b;
    }

    @Override
    public FileMapper[] getFileMappers() {
        return fileMappers;
    }

    @Override
    public void setFileMappers(final FileMapper[] fileMappers) {
        this.fileMappers = fileMappers;
    }

    @Override
    public final void extract() throws ArchiverException {
        resolvedDirectories = new LinkedHashMap<>(128, 0.75f, true);
        try {
            validate();
            execute();
            runArchiveFinalizers();
        } finally {
            resolvedDirectories = null;
        }
    }

    @Override
    public final void extract(final String path, final File outputDirectory) throws ArchiverException {
        resolvedDirectories = new LinkedHashMap<>(128, 0.75f, true);
        try {
            validate(path, outputDirectory);
            execute(path, outputDirectory);
            runArchiveFinalizers();
        } finally {
            resolvedDirectories = null;
        }
    }

    @Override
    public void addArchiveFinalizer(final ArchiveFinalizer finalizer) {
        if (finalizers == null) {
            finalizers = new ArrayList<>();
        }

        finalizers.add(finalizer);
    }

    @Override
    public void setArchiveFinalizers(final List<ArchiveFinalizer> archiveFinalizers) {
        finalizers = archiveFinalizers;
    }

    private void runArchiveFinalizers() throws ArchiverException {
        if (finalizers != null) {
            for (ArchiveFinalizer finalizer : finalizers) {
                finalizer.finalizeArchiveExtraction(this);
            }
        }
    }

    protected void validate(final String path, final File outputDirectory) {}

    protected void validate() throws ArchiverException {
        if (sourceFile == null) {
            throw new ArchiverException("The source file isn't defined.");
        }

        if (sourceFile.isDirectory()) {
            throw new ArchiverException("The source must not be a directory.");
        }

        if (!sourceFile.exists()) {
            throw new ArchiverException("The source file " + sourceFile + " doesn't exist.");
        }

        if (destDirectory == null && destFile == null) {
            throw new ArchiverException("The destination isn't defined.");
        }

        if (destDirectory != null && destFile != null) {
            throw new ArchiverException("You must choose between a destination directory and a destination file.");
        }

        try {
            // Use the same link/.. semantics for classification and extraction, retaining the configured spelling.
            if (destDirectory != null && !Files.isDirectory(resolveExtractionRoot(destDirectory))) {
                destFile = destDirectory;
                destDirectory = null;
            }

            if (destFile != null && Files.isDirectory(resolveExtractionRoot(destFile))) {
                destDirectory = destFile;
                destFile = null;
            }
        } catch (IOException e) {
            throw new ArchiverException("Cannot resolve extraction destination", e);
        }
    }

    @Override
    public void setFileSelectors(final FileSelector[] fileSelectors) {
        this.fileSelectors = fileSelectors;
    }

    @Override
    public FileSelector[] getFileSelectors() {
        return fileSelectors;
    }

    protected boolean isSelected(final String fileName, final PlexusIoResource fileInfo) throws ArchiverException {
        if (fileSelectors != null) {
            for (FileSelector fileSelector : fileSelectors) {
                try {

                    if (!fileSelector.isSelected(fileInfo)) {
                        return false;
                    }
                } catch (final IOException e) {
                    throw new ArchiverException(
                            "Failed to check, whether " + fileInfo.getName() + " is selected: " + e.getMessage(), e);
                }
            }
        }
        return true;
    }

    protected abstract void execute() throws ArchiverException;

    protected abstract void execute(String path, File outputDirectory) throws ArchiverException;

    /**
     * @since 1.1
     */
    @Override
    public boolean isIgnorePermissions() {
        return ignorePermissions;
    }

    /**
     * @since 1.1
     */
    @Override
    public void setIgnorePermissions(final boolean ignorePermissions) {
        this.ignorePermissions = ignorePermissions;
    }

    protected void extractFile(
            final File srcF,
            final File dir,
            final InputStream compressedInputStream,
            String entryName,
            final Date entryDate,
            final boolean isDirectory,
            final Integer mode,
            String symlinkDestination,
            final FileMapper[] fileMappers)
            throws IOException, ArchiverException {
        if (fileMappers != null) {
            for (final FileMapper fileMapper : fileMappers) {
                entryName = fileMapper.getMappedFileName(entryName);
            }
        }

        final boolean symlink = !StringUtils.isEmpty(symlinkDestination);
        Path destination = resolveExtractionPath(dir, entryName);
        checkFinalSymlink(destination, entryName, symlink);
        File targetFileName = destination.toFile();

        try {
            if (!shouldExtractEntry(dir, targetFileName, entryName, entryDate)) {
                return;
            }

            // create intermediary directories - sometimes zip don't add them
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            destination = resolveExtractionPath(dir, entryName);
            checkFinalSymlink(destination, entryName, symlink);
            targetFileName = destination.toFile();

            if (symlink) {
                SymlinkUtils.createSymbolicLink(targetFileName, new File(symlinkDestination));
            } else if (isDirectory) {
                Files.createDirectories(destination);
            } else {
                Files.copy(compressedInputStream, destination, REPLACE_EXISTING);
            }

            if (symlink) {
                // A link can point outside the extraction root. Never change its target's metadata.
                if (Files.isSymbolicLink(destination)) {
                    try {
                        BasicFileAttributeView attributes = Files.getFileAttributeView(
                                destination, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                        attributes.setTimes(FileTime.fromMillis(entryDate.getTime()), null, null);
                    } catch (IOException | UnsupportedOperationException ignored) {
                        // Link timestamps are best effort, like File.setLastModified for ordinary entries.
                    }
                }
            } else {
                targetFileName.setLastModified(entryDate.getTime());
            }

            if (!symlink && !isIgnorePermissions() && mode != null && !isDirectory) {
                ArchiveEntryUtils.chmod(targetFileName, mode);
            }
        } catch (final FileNotFoundException ex) {
            getLogger().warn("Unable to expand to file " + targetFileName.getPath());
        }
    }

    private static void checkFinalSymlink(Path destination, String entryName, boolean symlink) {
        if (!symlink && Files.isSymbolicLink(destination)) {
            throw new ArchiverException("Entry is outside of the target directory (" + entryName + ")");
        }
    }

    /**
     * Resolves the trusted root, following each link before processing subsequent parent components.
     * @param directory the configured root, whose original spelling remains available to extraction hooks
     * @return the physical root, including any not-yet-existing suffix
     * @throws IOException if an existing component cannot be resolved
     */
    protected final Path resolveExtractionRoot(File directory) throws IOException {
        return resolvePath(directory.toPath(), true);
    }

    /**
     * Resolves an entry's parents and checks containment, leaving a final symlink for entry-type validation.
     * @param directory the trusted extraction root
     * @param name the mapped destination name
     * @return a contained physical path without following its final symbolic link
     * @throws IOException if an existing component cannot be resolved
     * @throws ArchiverException if the destination is outside the root
     */
    protected final Path resolveExtractionPath(File directory, String name) throws IOException {
        Path root = resolveExtractionRoot(directory);
        // Preserve the previous resolver's portable absolute names and platform-native relative names.
        Path portable = Path.of(name.replace('/', File.separatorChar).replace('\\', File.separatorChar));
        // The root was just resolved above. Relative entries need only walk their own components.
        Path destination =
                portable.isAbsolute() ? resolvePath(portable, false) : resolvePath(root, Path.of(name), false);
        if (!destination.startsWith(root)) {
            throw new ArchiverException("Entry is outside of the target directory (" + name + ")");
        }
        return destination;
    }

    /**
     * Resolves existing components individually: Windows canonicalization of a missing leaf can leave its
     * symlinked parents unresolved, and whole-path normalization can erase a link before a later {@code ..}.
     */
    private Path resolvePath(Path path, boolean followLastLink) throws IOException {
        Path absolute = path.toAbsolutePath();
        return resolvePath(absolute.getRoot(), absolute, followLastLink);
    }

    /** Starts at a prefix already resolved during this check, without repeating its filesystem operations. */
    private Path resolvePath(Path current, Path path, boolean followLastLink) throws IOException {
        Deque<Path> remaining = new ArrayDeque<>();
        path.forEach(remaining::addLast);
        int links = 0;
        while (!remaining.isEmpty()) {
            Path name = remaining.removeFirst();
            if (name.toString().isEmpty() || name.toString().equals(".")) {
                continue;
            }
            if (name.toString().equals("..")) {
                if (current.getParent() != null) {
                    current = current.getParent();
                }
                continue;
            }
            Path candidate = current.resolve(name);
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(candidate, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException ignored) {
                current = candidate;
                continue;
            }
            if (attributes.isSymbolicLink()) {
                if (remaining.isEmpty() && !followLastLink) {
                    return candidate;
                }
                if (++links > 64) {
                    throw new FileSystemLoopException(path.toString());
                }
                // Resolve the target against the physical parent. Do not normalize away link/.. components.
                Path target = current.resolve(Files.readSymbolicLink(candidate)).toAbsolutePath();
                for (int i = target.getNameCount() - 1; i >= 0; i--) {
                    remaining.addFirst(target.getName(i));
                }
                current = target.getRoot();
            } else {
                if (!remaining.isEmpty() && !attributes.isDirectory()) {
                    throw new NotDirectoryException(candidate.toString());
                }
                // With no unresolved parent links or dots, this also handles Windows casing and short names.
                current = resolveDirectory(candidate, attributes);
            }
        }
        return current;
    }

    /** Avoids repeated real-path walks while still observing replacements and links on every visit. */
    private Path resolveDirectory(Path candidate, BasicFileAttributes attributes) throws IOException {
        Object key = attributes.fileKey();
        // Reparse points and providers without file identities need fresh resolution. Do not cache file leaves.
        if (resolvedDirectories == null || !attributes.isDirectory() || attributes.isOther() || key == null) {
            return candidate.toRealPath();
        }
        ResolvedDirectory cached = resolvedDirectories.get(candidate);
        if (cached != null && key.equals(cached.fileKey())) {
            return cached.path();
        }
        Path resolved = candidate.toRealPath();
        // Only retain spellings that already match the resolved path; aliases still need fresh resolution.
        if (candidate.equals(resolved)) {
            resolvedDirectories.put(candidate, new ResolvedDirectory(key, resolved));
            if (resolvedDirectories.size() > 1024) {
                var oldest = resolvedDirectories.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        } else {
            resolvedDirectories.remove(candidate);
        }
        return resolved;
    }

    /**
     * Counter for casing message emitted, visible for testing.
     */
    final AtomicInteger casingMessageEmitted = new AtomicInteger(0);

    // Visible for testing
    protected boolean shouldExtractEntry(File targetDirectory, File targetFileName, String entryName, Date entryDate)
            throws IOException {
        //     entryname  | entrydate | filename   | filedate | behavior
        // (1) readme.txt | 1970      | -          | -        | always extract if the file does not exist
        // (2) readme.txt | 1970      | readme.txt | 2020     | do not overwrite unless isOverwrite() is true
        // (3) readme.txt | 2020      | readme.txt | 1970     | always override when the file is older than the archive
        // entry
        // (4) README.txt | 1970      | readme.txt | 2020     | case-insensitive filesystem: warn + do not overwrite
        // unless isOverwrite()
        //                                                      case-sensitive filesystem: extract without warning
        // (5) README.txt | 2020      | readme.txt | 1970     | case-insensitive filesystem: warn + overwrite because
        // entry is newer
        //                                                      case-sensitive filesystem: extract without warning

        // The canonical file name follows the name of the archive entry, but takes into account the case-
        // sensitivity of the filesystem. So on a case-sensitive file system, file.exists() returns false for
        // scenario (4) and (5).
        // No matter the case sensitivity of the file system, file.exists() returns false when there is no file with the
        // same name (1).
        if (!targetFileName.exists()) {
            return true;
        }

        boolean entryIsDirectory =
                entryName.endsWith("/"); // directory entries always end with '/', regardless of the OS.
        String canonicalDestPath = resolvePath(targetFileName.toPath(), false).toString();
        String suffix = (entryIsDirectory ? "/" : "");
        String relativeCanonicalDestPath =
                canonicalDestPath.replace(resolveExtractionRoot(targetDirectory) + File.separator, "") + suffix;
        boolean fileOnDiskIsOlderThanEntry = targetFileName.lastModified() < entryDate.getTime();
        boolean differentCasing =
                !normalizedFileSeparator(entryName).equals(normalizedFileSeparator(relativeCanonicalDestPath));

        // Warn for case (4) and (5) if the file system is case-insensitive
        if (differentCasing) {
            String casingMessage = String.format(
                    Locale.ENGLISH,
                    "Archive entry '%s' and existing file '%s' names differ only by case."
                            + " This may lead to an unexpected outcome on case-insensitive filesystems.",
                    entryName,
                    canonicalDestPath);
            getLogger().warn(casingMessage);
            casingMessageEmitted.incrementAndGet();
        }

        // Override the existing file if isOverwrite() is true or if the file on disk is older than the one in the
        // archive
        return isOverwrite() || fileOnDiskIsOlderThanEntry;
    }

    private String normalizedFileSeparator(String pathOrEntry) {
        return pathOrEntry.replace("/", File.separator);
    }
}
