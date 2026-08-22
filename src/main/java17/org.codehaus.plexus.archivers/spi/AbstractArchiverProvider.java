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

import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverConfigurer;
import org.codehaus.plexus.archivers.internal.DefaultArchiverConfigurer;

/**
 * Base implementation that keeps unconfigured archiver creation internal to service providers.
 *
 * @since 5.0.0
 */
public abstract non-sealed class AbstractArchiverProvider implements ArchiverProvider {

    protected abstract Archiver createArchiver();

    @Override
    public final Archiver newArchiver(Consumer<ArchiverConfigurer> configurer) {
        Archiver archiver = createArchiver();
        Objects.requireNonNull(configurer, "configurer").accept(new DefaultArchiverConfigurer(archiver));
        return archiver;
    }
}
