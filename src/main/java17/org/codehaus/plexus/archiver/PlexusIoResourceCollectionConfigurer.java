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

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

/**
 * Configures a Plexus IO resource collection before it is exposed to callers.
 *
 * @since 5.0.0
 */
public interface PlexusIoResourceCollectionConfigurer {
    void setSource(Path source);

    void setPrefix(String prefix);

    void setIncludes(List<String> includes);

    void setExcludes(List<String> excludes);

    void setFileSelectors(List<FileSelector> fileSelectors);

    void setFileMappers(List<FileMapper> fileMappers);

    void setStreamTransformer(InputStreamTransformer streamTransformer);

    void setCaseSensitivity(CaseSensitivity caseSensitivity);

    void setDefaultExcludes(DefaultExcludes defaultExcludes);

    void setEmptyDirectoryHandling(EmptyDirectoryHandling emptyDirectoryHandling);

    void setEncoding(Charset charset);

    void setSymbolicLinkHandling(SymbolicLinkHandling symbolicLinkHandling);
}
