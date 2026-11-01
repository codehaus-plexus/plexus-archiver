/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;

/**
 * Settings for archive creation.
 *
 * <p>Multiplicity and precedence:
 * <ul>
 *   <li><b>addResource</b>: repeatable; resources are added in call order.</li>
 *   <li>All other setters are single-valued; last call wins (replace semantics).</li>
 *   <li>Operation-wide settings apply as defaults for all resources unless a
 *       lower-level setting is explicitly supported and set.</li>
 * </ul>
 *
 * @since 5.0.0
 */
public interface ArchiverSettings {

    static ArchiverSettings of(Archiver archiver) {
        return new DefaultArchiverSettings(archiver);
    }

    /**
     * Adds one source scan plus per-resource emit settings.
     *
     * <p>Repeatable.</p>
     */
    void addResource(ResourceScan scan, Consumer<ResourceEmitSettings> emit);

    /**
     * Destination archive file.
     *
     * <p>Single-valued; last call wins.</p>
     */
    void setDestFile(Path destFile);

    /**
     * Archive-wide default for empty directories.
     *
     * <p>Single-valued; last call wins.</p>
     */
    void setEmptyDirectoryHandling(EmptyDirectoryHandling handling);

    /**
     * Archive recreation policy.
     *
     * <p>Single-valued; last call wins.</p>
     */
    void setArchiveCreation(ArchiveCreation creation);

    /**
     * Duplicate entry policy.
     *
     * <p>Single-valued; last call wins.</p>
     */
    void setDuplicateHandling(DuplicateHandling handling);

    /**
     * Archive-wide default permission policy.
     *
     * <p>Single-valued; last call wins.</p>
     */
    void setPermissionPolicy(PermissionPolicy policy);
}