/**
 *
 * Copyright 2017 The Apache Software Foundation
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
package org.codehaus.plexus.archiver.jar.module;

/**
 * Holds the configuration for a module in modular JAR file.
 *
 * @since 3.6
 */
public class ModuleConfiguration
{

    /**
     * The module version.
     */
    private String version;

    /**
     * Returns the module version.
     *
      * @return The module version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Sets the module version.
     *
     * If set to {@code null}, the current version of the module will be retained.
     *
     * @param version The module version
     */
    public void setVersion( String version )
    {
        this.version = version;
    }

}
