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

package org.codehaus.plexus.archiver.manager;

import javax.inject.Provider;

import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archivers.internal.DefaultUnArchiverConfigurer;
import org.codehaus.plexus.archivers.spi.UnArchiverConfigurer;

final class CdiUnArchiverFactory implements UnArchiverFactory {
    private final Provider<UnArchiver> provider;

    CdiUnArchiverFactory(Provider<UnArchiver> provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public UnArchiver create(Consumer<UnArchiverConfigurer> configurer) {
        UnArchiver unarchiver = provider.get();
        Objects.requireNonNull(configurer, "configurer").accept(new DefaultUnArchiverConfigurer(unarchiver));
        return unarchiver;
    }
}
