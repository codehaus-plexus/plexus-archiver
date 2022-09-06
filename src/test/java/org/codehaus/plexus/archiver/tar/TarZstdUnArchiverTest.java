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
package org.codehaus.plexus.archiver.tar;

import java.io.File;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zstd.ZstdArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TarZstdUnArchiverTest extends TestSupport
{

    @Test
    public void testExtract()
        throws Exception
    {
        TarArchiver tarArchiver = (TarArchiver) lookup( Archiver.class, "tar" );
        tarArchiver.setLongfile( TarLongFileMode.posix );

        String fileName1 = "TarBZip2UnArchiverTest1.txt";
        String fileName2 = "TarBZip2UnArchiverTest2.txt";
        File file1InTar = getTestFile( "target/output/" + fileName1 );
        File file2InTar = getTestFile( "target/output/" + fileName2 );
        file1InTar.delete();
        file2InTar.delete();

        assertFalse( file1InTar.exists() );
        assertFalse( file2InTar.exists() );

        File testZstdFile = getTestFile( "target/output/archive.tar.zst" );
        if ( testZstdFile.exists() )
        {
            FileUtils.fileDelete( testZstdFile.getPath() );
        }
        assertFalse( testZstdFile.exists() );

        tarArchiver.addFile( getTestFile( "src/test/resources/manifests/manifest1.mf" ), fileName1 );
        tarArchiver.addFile( getTestFile( "src/test/resources/manifests/manifest2.mf" ), fileName2, 0664 );
        tarArchiver.setDestFile( getTestFile( "target/output/archive.tar" ) );
        tarArchiver.createArchive();

        ZstdArchiver zstdArchiver = (ZstdArchiver) lookup( Archiver.class, "zst" );

        zstdArchiver.setDestFile( testZstdFile );
        zstdArchiver.addFile( getTestFile( "target/output/archive.tar" ), "dontcare" );
        zstdArchiver.createArchive();

        assertTrue( testZstdFile.exists() );

        TarZstdUnArchiver tarZstdUnArchiver = (TarZstdUnArchiver) lookup( UnArchiver.class, "tar.zst" );

        tarZstdUnArchiver.setDestDirectory( getTestFile( "target/output" ) );
        tarZstdUnArchiver.setSourceFile( testZstdFile );
        tarZstdUnArchiver.extract();

        assertTrue( file1InTar.exists() );
        assertTrue( file2InTar.exists() );

        assertEquals( testZstdFile, tarZstdUnArchiver.getSourceFile() );
    }

    @Test
    public void testLookup() throws Exception
    {
        assertNotNull( lookup( UnArchiver.class, "tar.zst" ) );
    }

}
