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

package org.codehaus.plexus.archivers.spi;

/**
 * Controls whether an archive is always recreated or only when out of date.
 *
 * @since 5.0.0
 */
public sealed interface ArchiveCreation permits FixedArchiveCreation {
    ArchiveCreation ALWAYS = new FixedArchiveCreation(true);
    ArchiveCreation WHEN_NEEDED = new FixedArchiveCreation(false);
}

final class FixedArchiveCreation implements ArchiveCreation {
    final boolean forced;

    FixedArchiveCreation(boolean forced) {
        this.forced = forced;
    }
}
