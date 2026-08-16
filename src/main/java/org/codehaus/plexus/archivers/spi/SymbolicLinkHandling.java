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
 * Controls whether a filesystem resource collection follows symbolic links.
 *
 * @since 5.0.0
 */
public sealed interface SymbolicLinkHandling permits FixedSymbolicLinkHandling {
    SymbolicLinkHandling FOLLOW = new FixedSymbolicLinkHandling(true);
    SymbolicLinkHandling DO_NOT_FOLLOW = new FixedSymbolicLinkHandling(false);
}

final class FixedSymbolicLinkHandling implements SymbolicLinkHandling {
    final boolean follow;

    FixedSymbolicLinkHandling(boolean follow) {
        this.follow = follow;
    }
}
