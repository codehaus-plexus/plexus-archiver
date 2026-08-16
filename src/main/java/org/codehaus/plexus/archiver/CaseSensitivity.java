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

package org.codehaus.plexus.archiver;

/**
 * Controls whether file set patterns are case-sensitive.
 *
 * @since 5.0.0
 */
public sealed interface CaseSensitivity permits FixedCaseSensitivity, PlatformDefaultCaseSensitivity {
    CaseSensitivity SENSITIVE = new FixedCaseSensitivity(true);
    CaseSensitivity INSENSITIVE = new FixedCaseSensitivity(false);

    /**
     * Uses the platform's conventional file-system case sensitivity. This does not probe the file store and may differ
     * from the behavior of a particular mounted volume.
     */
    CaseSensitivity PLATFORM_DEFAULT = new PlatformDefaultCaseSensitivity();
}

final class FixedCaseSensitivity implements CaseSensitivity {
    final boolean caseSensitive;

    FixedCaseSensitivity(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}

final class PlatformDefaultCaseSensitivity implements CaseSensitivity {}
