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

import junit.framework.TestCase;

public class ModuleConfigurationTest
    extends TestCase
{

    public void testMainClass()
    {
        ModuleConfiguration config = new ModuleConfiguration();

        // Using both '.' and '/' should be legal...
        config.setMainClass( "com.example.Main" );
        config.setMainClass( "com/example/Main" );

        // ...as well as nested classes...
        config.setMainClass( "com.example.Main$Inner" );

        // ...and just in case lets verify that the main class is properly set
        assertEquals( "com.example.Main$Inner", config.getMainClass() );
    }

    public void testMainClassIllegalValues()
    {
        ModuleConfiguration config = new ModuleConfiguration();

        try
        {
            config.setMainClass( "" );
            fail( "Empty string is not a valid mainClass value." );
        }
        catch ( IllegalArgumentException ignore )
        {

        }

        try
        {
            config.setMainClass( "com.example.Ma[in" );
            fail( "'[' is not a valid character." );
        }
        catch ( IllegalArgumentException ignore )
        {

        }

        try
        {
            config.setMainClass( "com.example.Main;" );
            fail( "';' is not a valid character." );
        }
        catch ( IllegalArgumentException ignore )
        {

        }
    }

    public void testResetMainClassValue()
    {
        ModuleConfiguration config = new ModuleConfiguration();

        config.setMainClass( "com.example.Main" );
        assertNotNull( config.getMainClass() );

        config.setMainClass( null );
        assertNull( config.getMainClass() );
    }

}
