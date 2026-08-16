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

import org.codehaus.plexus.archiver.UnArchiver;

/**
 * Base implementation that keeps unconfigured unarchiver creation internal to service providers.
 *
 * @since 5.0.0
 */
public abstract non-sealed class AbstractUnArchiverProvider implements UnArchiverProvider {

    protected abstract UnArchiver createUnarchiver();

    @Override
    public final UnArchiver newUnarchiver(Consumer<UnArchiverConfigurer> configurer) {
        Objects.requireNonNull(configurer, "configurer");
        UnArchiver unarchiver = createUnarchiver();
        configurer.accept(new UnArchiverConfigurer(unarchiver));
        return unarchiver;
    }
}
