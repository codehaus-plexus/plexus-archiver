/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.jar;

/**
 * Exception thrown indicating problems in a JAR Manifest
 */
public class ManifestException extends Exception {

    /**
     * Constructs an exception with the given descriptive message.
     *
     * @param msg Description of or information about the exception.
     */
    public ManifestException(String msg) {
        super(msg);
    }
}
