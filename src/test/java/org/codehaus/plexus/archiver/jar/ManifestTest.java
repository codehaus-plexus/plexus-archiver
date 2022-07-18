/*
 * Copyright  2001,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.codehaus.plexus.archiver.jar;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.jar.Attributes;

import org.codehaus.plexus.archiver.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Venisse
 */
public class ManifestTest
        extends TestSupport
{

    @Test
    public void testManifest1()
        throws Exception
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifest1.mf" );
        String version = manifest.getManifestVersion();
        assertEquals( "1.0", version, "Manifest was not created with correct version - " );
    }

    @Test
    public void testManifest2()
        throws Exception
    {
        try
        {
            getManifest( "src/test/resources/manifests/manifest2.mf" );
            fail( "Manifest isn't well formed. It must be generate an exception." );
        }
        catch ( IOException ignore )
        {
        }
    }

    @Test
    public void testManifest3()
        throws Exception
    {
        try
        {
            getManifest( "src/test/resources/manifests/manifest3.mf" );
            fail( "Manifest isn't well formed. It must be generate an exception." );
        }
        catch ( IOException ignore )
        {
        }
    }

    @Test
    public void testManifest5()
        throws Exception
    {
        try
        {
            getManifest( "src/test/resources/manifests/manifest5.mf" );
            fail();
        }
        catch ( IOException ignore )
        {
        }
    }

    @Test
    public void testAddConfiguredSection()
        throws ManifestException
    {
        Manifest manifest = new Manifest();
        Manifest.Section section = new Manifest.Section();
        section.setName( "fud" );
        section.addConfiguredAttribute( new Manifest.Attribute( "bar", "baz" ) );
        manifest.addConfiguredSection( section );
        assertEquals( "baz", manifest.getAttributes( "fud" ).getValue( "bar" ) );
    }

    @Test
    public void testAttributeLongLineWrite()
        throws Exception
    {
        StringWriter writer = new StringWriter();
        Manifest.Attribute attr = new Manifest.Attribute();
        String longLineOfChars =
            "123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 123456789 123456789 ";
        attr.setName( "test" );
        attr.setValue( longLineOfChars );
        attr.write( writer );
        writer.flush();
        assertEquals( "test: 123456789 123456789 123456789 123456789 123456789 123456789 1234"
                          + Manifest.EOL + " 56789 123456789 123456789 123456789 " + Manifest.EOL,
                      writer.toString(), "should be multiline" );

    }

    @Test
    public void testAttributeLongLineWriteNonAscii()
        throws Exception
    {
        StringWriter writer = new StringWriter();
        Manifest.Attribute attr = new Manifest.Attribute();
        String longLineOfChars =
            "Ед докэндё форынчйбюж зкрипторэм векж, льабятюр ыкжпэтэндяз мэль ут, квюо ут модо "
                + "либриз такематыш. Ыюм йн лаборамюз компльыктётюр, векж ыпикурэи дежпютатионй ед,"
                + " ыам ты хабымуч мальюизчыт. Но вим алёэнюм вюльпутаты, ад нощтыр трётанё льаборэж"
                + " вэл, кевёбюж атоморюм кончюлату векж экз. Ку щольыат вёртюты ёнэрмйщ ыюм.";

        attr.setName( "test" );
        attr.setValue( longLineOfChars );
        attr.write( writer );
        writer.flush();
        assertEquals( "test: Ед докэндё форынчйбюж зкрипторэм в"
                          + Manifest.EOL + " екж, льабятюр ыкжпэтэндяз мэль ут, квю"
                          + Manifest.EOL + " о ут модо либриз такематыш. Ыюм йн лаб"
                          + Manifest.EOL + " орамюз компльыктётюр, векж ыпикурэи д"
                          + Manifest.EOL + " ежпютатионй ед, ыам ты хабымуч мальюи"
                          + Manifest.EOL + " зчыт. Но вим алёэнюм вюльпутаты, ад но"
                          + Manifest.EOL + " щтыр трётанё льаборэж вэл, кевёбюж ат"
                          + Manifest.EOL + " оморюм кончюлату векж экз. Ку щольыат "
                          + Manifest.EOL + " вёртюты ёнэрмйщ ыюм."
                          + Manifest.EOL,
                      writer.toString(), "should be multiline" );

    }

    @Test
    public void testDualClassPath()
        throws ManifestException, IOException
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifestWithDualClassPath.mf" );
        final String attribute = manifest.getMainSection().getAttributeValue( "Class-Path" );
        // According to discussions, we drop support for duplicate class-path attribute
        assertEquals( "baz", attribute );
    }

    @Test
    public void testAttributeMultiLineValue()
        throws Exception
    {
        checkMultiLineAttribute( "123456789" + Manifest.EOL + "123456789",
                                 "123456789" + Manifest.EOL + " 123456789" + Manifest.EOL );

    }

    @Test
    public void testAttributeDifferentLineEndings()
        throws Exception
    {
        checkMultiLineAttribute( "\tA\rB\n\t C\r\n \tD\n\r", "\tA" + Manifest.EOL + " B" + Manifest.EOL + " \t C"
                                                                 + Manifest.EOL + "  \tD" + Manifest.EOL );

    }

    @Test
    public void testAddAttributes()
        throws ManifestException, IOException
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifestMerge1.mf" );
        Manifest.ExistingSection fudz = manifest.getSection( "Fudz" );
        fudz.addConfiguredAttribute( new Manifest.Attribute( "boz", "bzz" ) );
        assertEquals( "bzz", fudz.getAttribute( "boz" ).getValue() );
        assertEquals( "bzz", manifest.getSection( "Fudz" ).getAttributeValue( "boz" ) );
    }

    @Test
    public void testRemoveAttributes()
        throws ManifestException, IOException
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifestMerge1.mf" );
        Manifest.ExistingSection fudz = manifest.getSection( "Fudz" );
        fudz.addConfiguredAttribute( new Manifest.Attribute( "boz", "bzz" ) );
        assertEquals( "bzz", fudz.getAttributeValue( "boz" ) );
        fudz.removeAttribute( "boz" );
        assertNull( fudz.getAttributeValue( "boz" ) );
    }

    @Test
    public void testAttributeSerialization()
        throws IOException, ManifestException
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue( "mfa1", "fud1" );
        manifest.getMainSection().addAttributeAndCheck( new Manifest.Attribute( "mfa2", "fud2" ) );
        Attributes attributes = new Attributes();
        attributes.putValue( "attA", "baz" );
        manifest.getEntries().put( "sub", attributes );
        manifest.getSection( "sub" ).addAttributeAndCheck( new Manifest.Attribute( "attB", "caB" ) );
        StringWriter writer = new StringWriter();
        manifest.write( writer );
        String s = writer.toString();
        assertTrue( s.contains( "mfa1: fud1" ) );
        assertTrue( s.contains( "mfa2: fud2" ) );
        assertTrue( s.contains( "attA: baz" ) );
        assertTrue( s.contains( "attB: caB" ) );
    }

    @Test
    public void testDefaultBehaviour()
    {
        Manifest manifest = new Manifest();
        Manifest.ExistingSection mainSection = manifest.getMainSection();
        assertNotNull( mainSection );
        String bar = mainSection.getAttributeValue( "Bar" );
        assertNull( bar );
        assertNull( manifest.getSection( "Fud" ) );
    }

    @Test
    public void testGetDefaultManifest()
        throws Exception
    {
        java.util.jar.Manifest mf = Manifest.getDefaultManifest();
        java.util.jar.Attributes mainAttributes = mf.getMainAttributes();
        assertEquals( 2, mainAttributes.size() );
        assertTrue( mainAttributes.containsKey( new java.util.jar.Attributes.Name( "Manifest-Version" ) ) );
        assertTrue( mainAttributes.containsKey( new java.util.jar.Attributes.Name( "Created-By" ) ) );

        mf = Manifest.getDefaultManifest( true );
        mainAttributes = mf.getMainAttributes();
        assertEquals( 1, mainAttributes.size() );
        assertTrue( mainAttributes.containsKey( new java.util.jar.Attributes.Name( "Manifest-Version" ) ) );
    }

    public void checkMultiLineAttribute( String in, String expected )
        throws Exception
    {
        StringWriter writer = new StringWriter();
        Manifest.Attribute attr = new Manifest.Attribute();
        attr.setName( "test" );
        attr.setValue( in );
        attr.write( writer );
        writer.flush();

        // Print the string with whitespace replaced with special codes
        // so in case of failure you can see what went wrong.
        System.err.println( "String: " + dumpString( writer.toString() ) );

        assertEquals( "test: " + expected, writer.toString(), "should be indented multiline" );
    }

    private static String dumpString( String in )
    {
        String out = "";

        char[] chars = in.toCharArray();

        for ( char aChar : chars )
        {
            switch ( aChar )
            {
                case '\t':
                    out += "\\t";
                    break;
                case '\r':
                    out += "\\r";
                    break;
                case '\n':
                    out += "\\n";
                    break;
                case ' ':
                    out += "\\s";
                    break;
                default:
                    out += aChar;
                    break;
            }
        }

        return out;
    }

    @Test
    public void testAddAttributesPlexusManifest()
        throws ManifestException, IOException
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifestMerge1.mf" );
        Manifest.ExistingSection fudz = manifest.getSection( "Fudz" );
        fudz.addConfiguredAttribute( new Manifest.Attribute( "boz", "bzz" ) );
        assertEquals( "bzz", manifest.getSection( "Fudz" ).getAttributeValue( "boz" ) );
    }

    @Test
    public void testRemoveAttributesPlexusManifest()
        throws ManifestException, IOException
    {
        Manifest manifest = getManifest( "src/test/resources/manifests/manifestMerge1.mf" );
        Manifest.ExistingSection fudz = manifest.getSection( "Fudz" );
        fudz.addConfiguredAttribute( new Manifest.Attribute( "boz", "bzz" ) );
        assertEquals( "bzz", fudz.getAttributeValue( "boz" ) );
        fudz.removeAttribute( "boz" );
        assertNull( fudz.getAttributeValue( "boz" ) );
    }

    @Test
    public void testAttributeSerializationPlexusManifest()
        throws IOException, ManifestException
    {
        Manifest manifest = new Manifest();
        manifest.getMainSection().addConfiguredAttribute( new Manifest.Attribute( "mfa1", "fud1" ) );
        manifest.getMainSection().addConfiguredAttribute( new Manifest.Attribute( "mfa2", "fud2" ) );
        Manifest.Section attributes = new Manifest.Section();
        attributes.setName( "TestSection" );
        attributes.addConfiguredAttribute( new Manifest.Attribute( "attA", "baz" ) );
        attributes.addConfiguredAttribute( new Manifest.Attribute( "attB", "caB" ) );
        manifest.addConfiguredSection( attributes );
        StringWriter writer = new StringWriter();
        manifest.write( writer );
        String s = writer.toString();
        assertTrue( s.contains( "mfa1: fud1" ) );
        assertTrue( s.contains( "mfa2: fud2" ) );
        assertTrue( s.contains( "attA: baz" ) );
        assertTrue( s.contains( "attB: caB" ) );
    }

    @Test
    public void testClassPathPlexusManifest()
        throws ManifestException
    {
        Manifest manifest = new Manifest();
        manifest.addConfiguredAttribute( new Manifest.Attribute( ManifestConstants.ATTRIBUTE_CLASSPATH, "fud" ) );
        manifest.addConfiguredAttribute( new Manifest.Attribute( ManifestConstants.ATTRIBUTE_CLASSPATH, "duf" ) );
        assertEquals( "fud duf", manifest.getMainSection().getAttributeValue( ManifestConstants.ATTRIBUTE_CLASSPATH ) );
    }

    @Test
    public void testAddConfiguredSectionPlexusManifest()
        throws ManifestException
    {
        Manifest manifest = new Manifest();
        Manifest.Section section = new Manifest.Section();
        section.setName( "fud" );
        section.addConfiguredAttribute( new Manifest.Attribute( "bar", "baz" ) );
        manifest.addConfiguredSection( section );
        assertEquals( "baz", manifest.getSection( "fud" ).getAttributeValue( "bar" ) );
    }

    /**
     * Reads a Manifest file.
     *
     * @param filename the file
     *
     * @return a manifest
     *
     * @throws java.io.IOException
     * @throws ManifestException
     */
    private Manifest getManifest( String filename )
        throws IOException, ManifestException
    {
        try ( InputStream is = Files.newInputStream( getTestFile( filename ).toPath() ) )
        {
            return new Manifest( is );
        }
    }

}
