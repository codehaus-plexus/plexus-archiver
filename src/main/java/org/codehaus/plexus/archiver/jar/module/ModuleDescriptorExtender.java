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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extends existing module descriptor with additional attributes.
 * If those attributes are already set they will be overridden.
 * The rest of the attributes will be retained.
 *
 * Not part of any public API.
 *
 * @since 3.6
 */
public class ModuleDescriptorExtender
{

    /**
     * The module version.
     */
    private String version;

    /**
     * The module main class.
     */
    private String mainClass;

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

    /**
     * Sets the module main class.
     *
     * If set to {@code null}, the current main class of the module will be retained.
     *
     * @param mainClass The qualified name of the main class
     */
    public void setMainClass( String mainClass )
    {
        this.mainClass = mainClass;
    }

    /**
     * Extends given module descriptor with
     * the configured in this instance attributes.
     *
     * @param moduleDescriptorInputStream The module descriptor to extend
     *
     * @return The resulting module descriptor
     *
     * @throws IOException If a problem occurs during reading the module descriptor
     */
    public byte[] extend( InputStream moduleDescriptorInputStream )
        throws IOException
    {
        ClassReader classReader = new ClassReader( moduleDescriptorInputStream );
        ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_FRAMES );

        ClassVisitor classVisitor = new ClassVisitor( Opcodes.ASM6, classWriter )
        {

            @Override
            public ModuleVisitor visitModule( String name, int access, String currentVersion )
            {
                String newVersion = version != null ? version : currentVersion;
                ModuleVisitor parentModuleVisitor = super.visitModule( name, access, newVersion );

                return new AttributeExtendingModuleVisitor( Opcodes.ASM6, parentModuleVisitor );
            }

        };

        classReader.accept( classVisitor, 0 );

        return classWriter.toByteArray();
    }

    /**
     * {@link ModuleVisitor} that sets the module main class.
     */
    private class AttributeExtendingModuleVisitor
        extends ModuleVisitor
    {

        AttributeExtendingModuleVisitor( int api, ModuleVisitor mv )
        {
            super( api, mv );
        }

        @Override
        public void visitMainClass( String currentMainClass )
        {
            // if mainClass is not set retain the original value
            // otherwise override in visitEnd()
            if ( mainClass == null )
            {
                super.visitMainClass( currentMainClass );
            }
        }

        @Override
        public void visitEnd()
        {
            // override the main class attribute if the mainClass is set
            if ( mainClass != null )
            {
                super.visitMainClass( mainClass.replace( '.', '/' ) );
            }

            super.visitEnd();
        }

    }

}
