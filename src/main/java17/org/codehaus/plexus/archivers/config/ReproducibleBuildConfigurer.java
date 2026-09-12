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

import java.nio.file.attribute.FileTime;

/**
 * Configures reproducible build content on an archiver without exposing its implementations.
 * 
 * NOTE: Based on {@link AbstractArchiver#configureReproducibleBuild(FileTime)} this could be extended with more options.
 * 
 * The default-prefix is used to make clear this is applied to all non-explicit entries. This leaves room to extend this class with different values for explicit entries
 * 
 * @since 5.0.0
 */
public interface ReproducibleBuildConfigurer {

	void setDefaultLastModifiedTime(FileTime fileTime);

}
