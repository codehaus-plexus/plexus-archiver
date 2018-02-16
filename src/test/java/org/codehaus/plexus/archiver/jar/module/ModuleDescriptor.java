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

import org.codehaus.plexus.util.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for the test to retrieve the module descriptor
 * from a module descriptor class.
 */
public class ModuleDescriptor
{
    private String version;
    private String mainClass;
    private List<String> requiredModules = new ArrayList<>();
    private List<String> exportedPackages = new ArrayList<>();

    private ModuleDescriptor()
    {
    }

    public String getVersion()
    {
        return version;
    }

    private void setVersion( String version )
    {
        this.version = version;
    }

    public String getMainClass()
    {
        return mainClass;
    }

    private void setMainClass( String mainClass )
    {
        this.mainClass = mainClass;
    }

    public List<String> getRequiredModules()
    {
        return requiredModules;
    }

    private void addRequiredModule( String requiredModule )
    {
        requiredModules.add( requiredModule );
    }

    public List<String> getExportedPackages()
    {
        return exportedPackages;
    }

    private void addExportedPackage( String exportedPackage )
    {
        exportedPackages.add( exportedPackage );
    }

    public static ModuleDescriptor read( URL moduleURL )
        throws IOException
    {
        try ( InputStream moduleInputStream = moduleURL.openStream() )
        {
            return ModuleDescriptor.read( IOUtil.toByteArray( moduleInputStream ) );
        }
    }

    public static ModuleDescriptor read( File moduleFile )
        throws IOException
    {
        try ( InputStream moduleInputStream = new FileInputStream( moduleFile ) )
        {
            return ModuleDescriptor.read( IOUtil.toByteArray( moduleInputStream ) );
        }
    }

    public static ModuleDescriptor read( byte[] module )
    {
        final ModuleDescriptor moduleDescriptor = new ModuleDescriptor();

        ClassReader classReader = new ClassReader( module );
        ClassVisitor classVisitor = new ClassVisitor( Opcodes.ASM6 )
        {

            @Override
            public ModuleVisitor visitModule( String name, int access, String version )
            {
                ModuleVisitor parentModuleVisitor = super.visitModule( name, access, version );

                moduleDescriptor.setVersion( version );

                return new ModuleVisitor( Opcodes.ASM6, parentModuleVisitor )
                {

                    @Override
                    public void visitMainClass( String mainClass )
                    {
                        super.visitMainClass( mainClass );

                        if ( moduleDescriptor.getMainClass() != null )
                        {
                            throw new RuntimeException( "Main class already set" );
                        }
                        moduleDescriptor.setMainClass( mainClass );
                    }

                    @Override
                    public void visitRequire( String requireModule, int access, String version )
                    {
                        super.visitRequire( requireModule, access, version );

                        moduleDescriptor.addRequiredModule( requireModule );
                    }

                    @Override
                    public void visitExport( String exportPackage, int access, String... modules )
                    {
                        super.visitExport( exportPackage, access, modules );

                        moduleDescriptor.addExportedPackage( exportPackage );
                    }

                };
            }

        };
        classReader.accept( classVisitor, 0 );

        return moduleDescriptor;
    }

}
