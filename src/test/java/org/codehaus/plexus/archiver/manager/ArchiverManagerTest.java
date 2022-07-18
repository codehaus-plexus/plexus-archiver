/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.codehaus.plexus.archiver.manager;

import java.io.File;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnArchiver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dan T. Tran
 */
public class ArchiverManagerTest
        extends TestSupport
{

    @Test
    public void testLookupArchiver()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        Archiver archiver = manager.getArchiver( "jar" );
        assertNotNull( archiver );
    }

    @Test
    public void testReuseArchiver()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        Archiver archiver = manager.getArchiver( "jar" );
        assertNotNull( archiver );

        archiver.addDirectory( new File( getBasedir() ) );

        Archiver newArchiver = manager.getArchiver( "jar" );
        assertNotNull( newArchiver );
        assertFalse( newArchiver.equals( archiver ) );

        assertTrue( !newArchiver.getResources().hasNext() );
    }

    @Test
    public void testLookupUnArchiver()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        UnArchiver unarchiver = manager.getUnArchiver( "zip" );
        assertNotNull( unarchiver );
    }

    @Test
    public void testLookupUnknownArchiver()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );
        try
        {
            manager.getArchiver( "Unknown" );
            fail();
        }
        catch ( NoSuchArchiverException ignore )
        {
        }
    }

    @Test
    public void testLookupUnknownUnArchiver()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );
        try
        {
            manager.getUnArchiver( "Unknown" );
            fail();
        }
        catch ( NoSuchArchiverException ignore )
        {
        }
    }

    @Test
    public void testLookupUnArchiverUsingFile()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        UnArchiver unarchiver = manager.getUnArchiver( new File( "test.tar.gz" ) );
        assertNotNull( unarchiver );

        unarchiver = manager.getUnArchiver( new File( "test.tar.bz2" ) );
        assertNotNull( unarchiver );

        unarchiver = manager.getUnArchiver( new File( "test.tgz" ) );
        assertNotNull( unarchiver );

        unarchiver = manager.getUnArchiver( new File( "test.tbz2" ) );
        assertNotNull( unarchiver );

        unarchiver = manager.getUnArchiver( new File( "test.bzip2" ) );
        assertNotNull( unarchiver );

        unarchiver = manager.getUnArchiver( new File( "test.tar" ) );
        assertNotNull( unarchiver );

    }

    @Test
    public void testLookupArchiverUsingFile()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        Archiver archiver = manager.getArchiver( new File( "test.gzip" ) );
        assertNotNull( archiver );

        archiver = manager.getArchiver( new File( "test.bzip2" ) );
        assertNotNull( archiver );

        archiver = manager.getArchiver( new File( "test.tar" ) );
        assertNotNull( archiver );

    }

    @Test
    public void testUnspportedLookupArchiverUsingFile()
        throws Exception
    {
        ArchiverManager manager = lookup( ArchiverManager.class );

        try
        {
            manager.getArchiver( new File( "test.tbz2" ) );
            //until we support this type, this must fail
            fail( "Please remove this test." );
        }
        catch ( NoSuchArchiverException ignore )
        {

        }

        try
        {
            manager.getArchiver( new File( "test.tgz" ) );
            //until we support this type, this must fail
            fail( "Please remove this test." );
        }
        catch ( NoSuchArchiverException ignore )
        {

        }

        try
        {
            manager.getArchiver( new File( "test.tar.gz" ) );
            //until we support this type, this must fail
            fail( "Please remove this test." );
        }
        catch ( NoSuchArchiverException ignore )
        {

        }

        try
        {
            manager.getArchiver( new File( "test.tar.bz2" ) );
            //until we support this type, this must fail
            fail( "Please remove this test." );
        }
        catch ( NoSuchArchiverException ignore )
        {

        }
    }

}
