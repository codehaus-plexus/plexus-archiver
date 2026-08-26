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

import java.util.function.Consumer;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archivers.config.ArchiverConfigurer;

/**
 * Creates configured archiver instances.
 *
 * @since 5.0.0
 */
@FunctionalInterface
public interface ArchiverFactory {
    /**
     * Creates a new archiver instance for manual configuration.
     * 
     * In legacy code, consumers mutate the returned archiver directly:
     * <pre>{@code
     * Archiver archiver = factory.create();
     * archiver.setDestFile(file);
     * archiver.addFileSet(fileSet);
     * archiver.createArchive();
     * }</pre>
     * 
     * @return a new mutable archiver instance
     */
    Archiver create();
    
    /**
     * Creates and configures an archiver using the configurer API.
     * 
     * <p>The returned archiver might be unmodifiable to prevent accidental mutations
     * after configuration is complete. In that case calling mutation methods will throw
     * {@link UnsupportedOperationException}.
     * 
     * Example:
     * <pre>{@code
     * Archiver configured = factory.configure(c -> c
     *     .setDestFile(outputJar)
     *     .addFileSetFromSpec(fileSetSpec));
     * configured.createArchive(); // OK - read-only operation
     * configured.addFileSet(other); // UnsupportedOperationException
     * }</pre>
     * 
     * @param configurer configuration callback
     * @return an unmodifiable archiver instance
     * @throws UnsupportedOperationException if mutation methods are called
     * @since 5.0.0
     */
    default Archiver configure(Consumer<ArchiverConfigurer> configurer) {
        Archiver archiver = create();
        configurer.accept(ArchiverConfigurer.of(archiver));
        return archiver;
    }
}
