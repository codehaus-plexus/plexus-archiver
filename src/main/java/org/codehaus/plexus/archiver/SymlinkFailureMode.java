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
package org.codehaus.plexus.archiver;

/**
 * What an {@link UnArchiver} does with a symbolic link entry when the file system refuses to create the link,
 * for example on Windows without the "Create symbolic links" right or Developer Mode.
 *
 * @since 4.15.0
 */
public enum SymlinkFailureMode {
    /**
     * Fail the extraction.
     */
    FAIL,

    /**
     * Log a warning and, once every entry is extracted, copy the link target in place of the link.
     * Targets outside the destination directory, missing targets and link cycles are skipped with a warning.
     */
    COPY_TARGET,

    /**
     * Log a warning and leave the entry out.
     */
    SKIP
}
