/*
 * Copyright 2026 The plexus developers.
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
package org.codehaus.plexus.archiver.tar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarConstants;

/** Resolves TAR member occurrences independently of output names and stream position. */
final class TarEntryIndex {
    private final List<TarArchiveEntry> entries = new ArrayList<>();
    private final Map<TarArchiveEntry, Integer> positions = new IdentityHashMap<>();
    private final Map<String, List<Integer>> names = new HashMap<>();
    private final Map<Integer, Integer> resolved = new HashMap<>();

    /** Records one header as a cursor encounters it, retaining occurrence identity on replay. */
    TarArchiveEntry record(int position, TarArchiveEntry entry) throws IOException {
        if (position < entries.size()) {
            TarArchiveEntry known = entries.get(position);
            if (!known.getName().equals(entry.getName())
                    || known.getSize() != entry.getSize()
                    || known.getLinkFlag() != entry.getLinkFlag()
                    || !known.getLinkName().equals(entry.getLinkName())) {
                throw new IOException("TAR changed while reading " + entry.getName());
            }
            return known;
        }
        entries.add(entry);
        positions.put(entry, position);
        names.computeIfAbsent(normalizedName(entry.getName()), ignored -> new ArrayList<>())
                .add(position);
        return entry;
    }

    /** Finds an observed occurrence, or the first matching name for caller-created headers. */
    Integer find(TarArchiveEntry entry) {
        Integer position = positions.get(entry);
        if (position != null) {
            return position;
        }
        List<Integer> matches = names.get(normalizedName(entry.getName()));
        return matches == null ? null : matches.get(0);
    }

    /** Returns an observed header by its position in the source archive. */
    TarArchiveEntry entry(int position) {
        return entries.get(position);
    }

    /** Returns an observed member's occurrence number without reading the archive. */
    int position(TarArchiveEntry entry) throws IOException {
        Integer position = find(entry);
        if (position == null) {
            throw new IOException("Unknown TAR entry: " + entry.getName());
        }
        return position;
    }

    /** Binds a hard link to the nearest earlier target, never to a later or self occurrence. */
    TarArchiveEntry linkTarget(TarArchiveEntry link) throws IOException {
        validateLinkName(link.getName());
        validateLinkName(link.getLinkName());
        String targetName = normalizedName(link.getLinkName());
        if (targetName.equals(normalizedName(link.getName()))) {
            throw new IOException("Self-referencing TAR hard link: " + link.getName());
        }
        List<Integer> matches = names.get(targetName);
        if (matches != null) {
            int found = Collections.binarySearch(matches, position(link));
            int before = found >= 0 ? found - 1 : -found - 2;
            if (before >= 0) {
                return entries.get(matches.get(before));
            }
        }
        throw new IOException("TAR hard-link target must precede " + link.getName() + ": " + link.getLinkName());
    }

    /** Resolves chains iteratively, memoizing the payload to keep long chains inexpensive. */
    TarArchiveEntry resolve(TarArchiveEntry entry) throws IOException {
        int current = position(entry);
        Set<Integer> visited = new HashSet<>();
        // Bind backward references to the version visible at that point in the archive.
        while (entries.get(current).isLink()) {
            if (resolved.containsKey(current)) {
                current = resolved.get(current);
                break;
            }
            if (!visited.add(current)) {
                throw new IOException("Cyclic TAR hard link: " + entry.getName());
            }
            // Each step moves backward, so invalid forward references cannot be repaired by replay.
            current = position(linkTarget(entries.get(current)));
        }
        if (!visited.isEmpty() && !isRegular(entries.get(current))) {
            throw new IOException("TAR hard link does not reference a regular file: " + entry.getName());
        }
        if (!visited.isEmpty()) {
            validateLinkName(entries.get(current).getName());
        }
        for (Integer link : visited) {
            resolved.put(link, current);
        }
        return entries.get(current);
    }

    /** Accepts data-bearing file types, excluding devices and symbolic links. */
    static boolean isRegular(TarArchiveEntry entry) {
        return !entry.isDirectory()
                && (entry.getLinkFlag() == TarConstants.LF_NORMAL
                        || entry.getLinkFlag() == TarConstants.LF_OLDNORM
                        || entry.isSparse());
    }

    /** Rejects filesystem-like escapes before interpreting a hard-link relationship. */
    private static void validateLinkName(String name) throws IOException {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) {
            throw new IOException("Unsafe TAR hard-link name: " + name);
        }
        for (String component : name.split("/")) {
            if (component.equals("..")) {
                throw new IOException("Unsafe TAR hard-link name: " + name);
            }
        }
    }

    /** Treats redundant slashes and dot components as the same archive-root-relative name. */
    static String normalizedName(String name) {
        List<String> components = new ArrayList<>();
        for (String component : name.split("/")) {
            if (!component.isEmpty() && !component.equals(".")) {
                components.add(component);
            }
        }
        return String.join("/", components);
    }
}
