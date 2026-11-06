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
package org.codehaus.plexus.archivers.config;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

/**
 * Per-resource emit settings for a single {@link ResourceScan}.
 *
 * <p>All setters are single-valued; last call wins.</p>
 *
 * @since 5.0.0
 */
public interface ResourceEmitSettings {

    void setPrefix(String prefix);

    void setEmptyDirectoryHandling(EmptyDirectoryHandling handling);

    /**
     * Ordered file mappers; applied in list order.
     */
    void setFileMappers(FileMapper... fileMappers);

    void setStreamTransformer(InputStreamTransformer transformer);
}