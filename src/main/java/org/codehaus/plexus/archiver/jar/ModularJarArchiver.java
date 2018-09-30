/**
 *
 * Copyright 2018 The Apache Software Foundation
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
 * Base class for creating modular JAR archives.
 *
 * Subclasses are required to be able to handle both
 * JAR archives with module descriptor (modular JAR)
 * and without ("regular" JAR).
 * That would allow clients of this class to use
 * it without prior knowledge if the classes
 * they are going to add are part of module
 * (contain module descriptor class) or not.
 *
 * <p>The class allows you to set the
 * module main class ({@link #setModuleMainClass(String)}),
 * but if it is not set or it is set to {@code null},
 * then the {@code Main-Class} attribute of the
 * JAR manifest is used (if present) to set
 * the module main class.
 *
 * @since 3.6
 */
public abstract class ModularJarArchiver
    extends JarArchiver
{
    private String moduleMainClass;

    private String manifestMainClass;

    private String moduleVersion;

    public String getModuleMainClass()
    {
        return moduleMainClass;
    }

    /**
     * Sets the module main class.
     * Ignored if the JAR file does not contain
     * module descriptor.
     *
     * <p>Note that implementations may choose
     * to replace the value set in the manifest as well.
     *
     * @param moduleMainClass the module main class.
     */
    public void setModuleMainClass( String moduleMainClass )
    {
        this.moduleMainClass = moduleMainClass;
    }

    public String getModuleVersion()
    {
        return moduleVersion;
    }

    /**
     * Sets the module version.
     * Ignored if the JAR file does not contain
     * module descriptor.
     *
     * @param moduleVersion the module version.
     */
    public void setModuleVersion( String moduleVersion )
    {
        this.moduleVersion = moduleVersion;
    }

    /**
     * Returns the "Main-Class" attribute of the
     * manifest added to the archive.
     *
     * {@code null} if there is no manifest
     * or the attribute is not set.
     *
     * @return the "Main-Class" attribute of the manifest
     */
    protected String getManifestMainClass()
    {
        return manifestMainClass;
    }

    @Override
    protected Manifest createManifest()
    {
        Manifest manifest = super.createManifest();

        if ( manifest != null )
        {
            manifestMainClass = manifest.getMainAttributes()
                .getValue( "Main-Class" );
        }

        return manifest;
    }

    @Override
    public void reset()
    {
        // We want to be sure that on multiple run
        // the latest manifest is used, so lets
        // reset it to null
        manifestMainClass = null;
        super.reset();
    }
}
