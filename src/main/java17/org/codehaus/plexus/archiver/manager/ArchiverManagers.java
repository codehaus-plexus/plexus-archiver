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

/**
 * Factory for creating {@link ArchiverManager} instances configured 
 * via ServiceLoader discovery of {@link ArchiverProvider}, 
 * {@link UnArchiverProvider}, and {@link PlexusIoResourceCollectionProvider}.
 * 
 * <p>This is the recommended way to obtain an ArchiverManager for standard
 * usage. For custom implementations, construct them directly.
 *
 * @since 5.0.0
 */
public final class ArchiverManagers {

	private ArchiverManagers() {
	}
	
	public static ArchiverManager create() {
		return new ServiceLoaderArchiverManager();
	}
	
	public static ArchiverManager create(ClassLoader classLoader) {
		return new ServiceLoaderArchiverManager(classLoader);
	}

	public static ArchiverManager create(ModuleLayer moduleLayer) {
		return new ServiceLoaderArchiverManager(moduleLayer);
	}

}
