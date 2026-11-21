/*
 * Copyright The Plexus developers.
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

/**
 * What a TAR extractor does when the file system refuses a valid hard link.
 *
 * @since 5.0.0
 */
public enum HardLinkFailureMode {
    /** Fail extraction immediately. */
    FAIL,

    /**
     * Log a warning and copy the target's final contents after the TAR entries have been extracted.
     * Queued targets that become invalid or missing and unresolved copy dependencies are skipped with a warning.
     */
    COPY_TARGET,

    /** Log a warning and leave the entry out. */
    SKIP
}
