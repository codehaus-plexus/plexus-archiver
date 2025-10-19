/*
 * Copyright 2007 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.jar;

import javax.inject.Named;

import org.codehaus.plexus.archiver.zip.PlexusIoJarFileResourceCollectionWithSignatureVerification;

/**
 * Resource collection for JAR files that uses JarFile to verify signatures when present.
 */
@Named("jar")
public class PlexusIoJarFileResourceCollection extends PlexusIoJarFileResourceCollectionWithSignatureVerification {}
