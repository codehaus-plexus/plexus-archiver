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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class ModuleDescriptorExtenderTest
    extends TestCase
{

    public void testExtendModuleDeclaration()
        throws IOException
    {
        File moduleDeclarationFile = new File( "src/test/resources/java-module/module-info.class" );

        // verify that the module version and the main class are not set
        verifyModule( moduleDeclarationFile, null, null,
            Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );

        ModuleDescriptorExtender extender = new ModuleDescriptorExtender();
        extender.setVersion( "1.2.3" );
        extender.setMainClass( "com.example.app.Main" );

        try ( InputStream moduleInputStream = new FileInputStream( moduleDeclarationFile ) )
        {
            byte[] result = extender.extend( moduleInputStream );
            // verify that the module version and the main class are set to the correct values
            // and the rest of module attributes are retained
            verifyModule( result, "1.2.3", "com/example/app/Main",
                Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );
        }
    }

    public void testExtendModuleDeclarationOverridesExistingValues()
        throws IOException
    {
        File moduleDeclarationFile = new File( "src/test/resources/java-module-extended/module-info.class" );

        // verify that the module version and the main class are set
        verifyModule( moduleDeclarationFile, "1.0", "com/example/app/Main",
            Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );

        ModuleDescriptorExtender extender = new ModuleDescriptorExtender();
        extender.setVersion( "2.0-Beta" );
        extender.setMainClass( "com.example.app.Main2" );

        try ( InputStream moduleInputStream = new FileInputStream( moduleDeclarationFile ) )
        {
            byte[] result = extender.extend( moduleInputStream );
            // verify that the module version and the main class are overridden
            verifyModule( result, "2.0-Beta", "com/example/app/Main2",
                Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );
        }
    }

    public void testExtendModuleDeclarationRetainsOriginalValues()
        throws IOException
    {
        File moduleDeclarationFile = new File( "src/test/resources/java-module-extended/module-info.class" );

        verifyModule( moduleDeclarationFile, "1.0", "com/example/app/Main",
            Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );

        // we don't extend any attribute
        ModuleDescriptorExtender extender = new ModuleDescriptorExtender();

        try ( InputStream moduleInputStream = new FileInputStream( moduleDeclarationFile ) )
        {
            byte[] result = extender.extend( moduleInputStream );
            // verify that the module attributes are not changed
            verifyModule( result, "1.0", "com/example/app/Main",
                Collections.singletonList( "java.base" ), Collections.singletonList( "com/example/app" ) );
        }
    }

    private void verifyModule( File moduleDescriptorFile,
                               String expectedVersion, String expectedMainClass,
                               List<String> expectedRequiredModules, List<String> expectedExportedPackages )
        throws IOException
    {
        ModuleDescriptor moduleDescriptor = ModuleDescriptor.read( moduleDescriptorFile );

        verifyModule( moduleDescriptor,
            expectedVersion, expectedMainClass, expectedRequiredModules, expectedExportedPackages );
    }

    private void verifyModule( byte[] moduleDescriptorBytes,
                               String expectedVersion, String expectedMainClass,
                               List<String> expectedRequiredModules, List<String> expectedExportedPackages )
    {
        ModuleDescriptor moduleDescriptor = ModuleDescriptor.read( moduleDescriptorBytes );

        verifyModule( moduleDescriptor,
            expectedVersion, expectedMainClass, expectedRequiredModules, expectedExportedPackages );
    }

    private void verifyModule( ModuleDescriptor moduleDescriptor,
                               String expectedVersion, String expectedMainClass,
                               List<String> expectedRequiredModules, List<String> expectedExportedPackages )
    {
        assertEquals( expectedVersion, moduleDescriptor.getVersion() );
        assertEquals( expectedMainClass, moduleDescriptor.getMainClass() );
        assertEquals( expectedRequiredModules, moduleDescriptor.getRequiredModules() );
        assertEquals( expectedExportedPackages, moduleDescriptor.getExportedPackages() );
    }

}
