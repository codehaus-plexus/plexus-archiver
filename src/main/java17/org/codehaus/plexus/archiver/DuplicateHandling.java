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
 * Controls how duplicate archive entries are handled.
 *
 * @since 5.0.0
 */
public sealed interface DuplicateHandling permits FixedDuplicateHandling {
    DuplicateHandling ADD = new FixedDuplicateHandling(Archiver.DUPLICATES_ADD);
    DuplicateHandling PRESERVE = new FixedDuplicateHandling(Archiver.DUPLICATES_PRESERVE);
    DuplicateHandling SKIP = new FixedDuplicateHandling(Archiver.DUPLICATES_SKIP);
    DuplicateHandling FAIL = new FixedDuplicateHandling(Archiver.DUPLICATES_FAIL);
}

final class FixedDuplicateHandling implements DuplicateHandling {
    final String value;

    FixedDuplicateHandling(String value) {
        this.value = value;
    }
}
