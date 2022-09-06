/*
 * Copyright 2022 The Apache Software Foundation
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
package org.codehaus.plexus.archiver.zstd;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.BasePlexusArchiverTest;
import org.codehaus.plexus.archiver.exceptions.EmptyArchiveException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ZstdArchiverTest extends BasePlexusArchiverTest
{

    @Test
    public void testCreateArchive()
        throws Exception
    {
        ZipArchiver zipArchiver = (ZipArchiver) lookup( Archiver.class, "zip" );
        zipArchiver.addDirectory( getTestFile( "src" ) );
        zipArchiver.setDestFile( getTestFile( "target/output/archiveForxz.zip" ) );
        zipArchiver.createArchive();

        ZstdArchiver archiver = (ZstdArchiver) lookup( Archiver.class, "zst" );
        String[] inputFiles = new String[ 1 ];
        inputFiles[0] = "archiveForxz.zip";

        File targetOutputFile = getTestFile( "target/output/archive.zst" );
        if ( targetOutputFile.exists() )
        {
            FileUtils.fileDelete( targetOutputFile.getPath() );
        }
        assertFalse( targetOutputFile.exists() );

        archiver.addDirectory( getTestFile( "target/output" ), inputFiles, null );
        archiver.setDestFile( targetOutputFile );
        archiver.createArchive();

        assertTrue( targetOutputFile.exists() );
    }

    @Test
    public void testCreateEmptyArchive()
        throws Exception
    {
        ZstdArchiver archiver = (ZstdArchiver) lookup( Archiver.class, "zst" );
        archiver.setDestFile( getTestFile( "target/output/empty.zst" ) );
        try
        {
            archiver.createArchive();

            fail( "Creating empty archive should throw EmptyArchiveException" );
        }
        catch ( EmptyArchiveException ignore )
        {
        }
    }

    @Test
    public void testCreateResourceCollection() throws Exception
    {
        final File pomFile = new File( "pom.xml" );
        final File zstFile = new File( "target/output/pom.xml.zst" );
        ZstdArchiver zstdArchiver = (ZstdArchiver) lookup( Archiver.class, "zst" );
        zstdArchiver.setDestFile( zstFile );
        zstdArchiver.addFile( pomFile, "pom.xml" );
        FileUtils.removePath( zstFile.getPath() );
        zstdArchiver.createArchive();

        System.out.println( "Created: " + zstFile.getAbsolutePath() );

        final File zipFile = new File( "target/output/pomxz.zip" );
        ZipArchiver zipArchiver = (ZipArchiver) lookup( Archiver.class, "zip" );
        zipArchiver.setDestFile( zipFile );
        zipArchiver.addArchivedFileSet( zstFile, "prfx/" );
        FileUtils.removePath( zipFile.getPath() );
        zipArchiver.createArchive();

        final ZipFile juZipFile = new ZipFile( zipFile );
        final ZipEntry zipEntry = juZipFile.getEntry( "prfx/target/output/pom.xml" );
        final InputStream archivePom = juZipFile.getInputStream( zipEntry );
        final InputStream pom = Files.newInputStream( pomFile.toPath() );

        assertTrue( Arrays.equals( IOUtil.toByteArray( pom ), IOUtil.toByteArray( archivePom ) ) );
        archivePom.close();
        pom.close();
        juZipFile.close();
    }

    /**
     * Tests the .std archiver is forced set to true, and after that
     * tests the behavior when the forced is set to false.
     *
     * @throws Exception
     */
    @Test
    public void testZstIsForcedBehaviour() throws Exception
    {
        ZstdArchiver zstdArchiver = (ZstdArchiver) createArchiver( "zst" );

        assertTrue( zstdArchiver.isSupportingForced() );
        zstdArchiver.createArchive();

        final long creationTime = zstdArchiver.getDestFile().lastModified();

        waitUntilNewTimestamp( zstdArchiver.getDestFile(), creationTime );

        zstdArchiver = (ZstdArchiver) createArchiver( "zst" );

        zstdArchiver.setForced( true );
        zstdArchiver.createArchive();

        final long firstRunTime = zstdArchiver.getDestFile().lastModified();

        assertFalse( creationTime == firstRunTime );

        zstdArchiver = (ZstdArchiver) createArchiver( "zst" );

        zstdArchiver.setForced( false );
        zstdArchiver.createArchive();

        final long secondRunTime = zstdArchiver.getDestFile().lastModified();

        assertEquals( firstRunTime, secondRunTime );
    }

}
