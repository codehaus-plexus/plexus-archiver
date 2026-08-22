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

import java.nio.file.Path;
import java.util.List;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

/**
 * Configures an unarchiver before it is exposed to callers.
 *
 * @since 5.0.0
 */
public interface UnArchiverConfigurer {
    void setSource(Path source);

    void setDestinationDirectory(Path destinationDirectory);

    void setDestinationFile(Path destinationFile);

    void setExistingFileHandling(ExistingFileHandling existingFileHandling);

    void setFileMappers(List<FileMapper> fileMappers);

    void setFileSelectors(List<FileSelector> fileSelectors);

    void setPermissionHandling(PermissionHandling permissionHandling);
}
