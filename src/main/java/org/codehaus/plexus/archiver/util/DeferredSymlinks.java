/*
 * Copyright The Plexus developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

/**
 * Symbolic links the file system refused, to be replaced by a copy of their target once every entry is on disk.
 *
 * @see org.codehaus.plexus.archiver.SymlinkFailureMode#COPY_TARGET
 * @since 4.15.0
 */
public final class DeferredSymlinks {

    private final Logger logger;

    private final List<Entry> entries = new ArrayList<>();

    public DeferredSymlinks(Logger logger) {
        this.logger = logger;
    }

    /**
     * Records a link that could not be created.
     *
     * @param root the canonical directory the copy must stay inside
     * @param link where the link would have been created; its parent directory must exist
     * @param target the link target as stored in the archive or resource
     * @param entryName the entry name, for messages
     * @throws IOException if the parent directory cannot be canonicalized
     */
    public void add(Path root, File link, String target, String entryName) throws IOException {
        // canonicalize the parent, not the link path, which might be an existing link
        Path linkPath = link.getParentFile().getCanonicalFile().toPath().resolve(link.getName());
        entries.add(new Entry(root, linkPath, target, entryName));
    }

    /**
     * Copies the target of each recorded link in place of the link. Repeats until no more links resolve, so links
     * that precede their target and chains of links work in any order. Targets outside the root, missing targets,
     * cycles, occupied link paths and directories that contain the link are skipped with a warning.
     *
     * @throws IOException if copying fails
     */
    public void resolve() throws IOException {
        List<Entry> pending = new ArrayList<>(entries);
        entries.clear();
        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            for (Iterator<Entry> it = pending.iterator(); it.hasNext(); ) {
                Entry link = it.next();
                Path real;
                try {
                    real = link.link
                            .resolveSibling(link.target.replace('/', File.separatorChar))
                            .normalize()
                            .toRealPath();
                } catch (NoSuchFileException e) {
                    // the target may be another refused link that a later round copies
                    continue;
                }
                it.remove();
                if (!real.startsWith(link.root)) {
                    logger.warn(
                            "Skipping symbolic link {}: target {} is outside the target directory",
                            link.entryName,
                            link.target);
                } else if (Files.exists(link.link, LinkOption.NOFOLLOW_LINKS)) {
                    logger.warn("Skipping symbolic link {}: {} already exists", link.entryName, link.link);
                } else if (Files.isDirectory(real) && link.link.startsWith(real)) {
                    logger.warn("Skipping symbolic link {}: target {} contains the link", link.entryName, link.target);
                } else {
                    copy(real, link.link);
                    progress = true;
                }
            }
        }
        for (Entry link : pending) {
            logger.warn(
                    "Skipping symbolic link {}: target {} is not in the archive, or the links form a cycle",
                    link.entryName,
                    link.target);
        }
    }

    private void copy(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source)) {
            Files.copy(source, destination, COPY_ATTRIBUTES);
            return;
        }
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) {
                    logger.warn("Skipping symbolic link {} while copying {}", file, source);
                } else {
                    Files.copy(file, destination.resolve(source.relativize(file)), COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class Entry {
        final Path root;

        final Path link;

        final String target;

        final String entryName;

        Entry(Path root, Path link, String target, String entryName) {
            this.root = root;
            this.link = link;
            this.target = target;
            this.entryName = entryName;
        }
    }
}
