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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.codehaus.plexus.archiver.jar.harmony.Attributes;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.util.PropertyUtils;

/**
 * Not part of any public API
 *
 * @author Kristian Rosenvold
 */
class JdkManifestFactory
{

    public static org.codehaus.plexus.archiver.jar.harmony.Manifest getDefaultManifest()
        throws ArchiverException
    {
        final org.codehaus.plexus.archiver.jar.harmony.Manifest defaultManifest = new org.codehaus.plexus.archiver.jar.harmony.Manifest();
        defaultManifest.getMainAttributes().putValue( "Manifest-Version", "1.0" );

        String createdBy = "Plexus Archiver";

        final String plexusArchiverVersion = getArchiverVersion();

        if ( plexusArchiverVersion != null )
        {
            createdBy += " " + plexusArchiverVersion;
        }

        defaultManifest.getMainAttributes().putValue( "Created-By", createdBy );
        return defaultManifest;
    }

    static String getArchiverVersion()
    {
        try
        {
            final Properties properties = PropertyUtils.loadProperties( JdkManifestFactory.class.getResourceAsStream(
                "/META-INF/maven/org.codehaus.plexus/plexus-archiver/pom.properties" ) );

            return properties != null
                       ? properties.getProperty( "version" )
                       : null;

        }
        catch ( final IOException e )
        {
            throw new AssertionError( e );
        }
    }

    public static void merge( org.codehaus.plexus.archiver.jar.harmony.Manifest target, org.codehaus.plexus.archiver.jar.harmony.Manifest other, boolean overwriteMain )
    {
        if ( other != null )
        {
            final Attributes mainAttributes = target.getMainAttributes();
            if ( overwriteMain )
            {
                mainAttributes.clear();
                mainAttributes.putAll( other.getMainAttributes() );
            }
            else
            {
                mergeAttributes( mainAttributes, other.getMainAttributes() );
            }

            for ( Map.Entry<String, Attributes> o : other.getEntries().entrySet() )
            {
                Attributes ourSection = target.getAttributes( o.getKey() );
                Attributes otherSection = o.getValue();
                if ( ourSection == null )
                {
                    if ( otherSection != null )
                    {
                        target.getEntries().put( o.getKey(), (Attributes) otherSection.clone() );
                    }
                }
                else
                {
                    mergeAttributes( ourSection, otherSection );
                }
            }
        }
    }

    /**
     * Merge in another section
     *
     * @param target The target manifest of the merge
     * @param section the section to be merged with this one.
     */
    public static void mergeAttributes( org.codehaus.plexus.archiver.jar.harmony.Attributes target, org.codehaus.plexus.archiver.jar.harmony.Attributes section )
    {
        for ( Object o : section.keySet() )
        {
            org.codehaus.plexus.archiver.jar.harmony.Attributes.Name key = (Attributes.Name) o;
            final Object value = section.get( o );
            // the merge file always wins
            target.put( key, value );
        }
    }

}
