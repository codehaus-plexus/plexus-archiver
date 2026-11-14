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

/**
 * Service provider interfaces through which {@link org.codehaus.plexus.archiver.manager.ServiceLoaderArchiverManager}
 * finds archivers, unarchivers and resource collections.
 *
 * <p>These interfaces are internal to plexus-archiver: they exist so that the formats shipped in this library can be
 * found without a dependency injection container. They are not meant to be implemented outside plexus-archiver and
 * change incompatibly in 5.0, where they are sealed and their factory methods take a configurer. Code that needs an
 * {@link org.codehaus.plexus.archiver.manager.ArchiverManager} without a container should create a
 * {@code ServiceLoaderArchiverManager}, which is the same on 4.x and 5.0.
 */
package org.codehaus.plexus.archivers.spi;
