/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.output.ThresholdingOutputStream;

/**
 * Offloads to disk when a given memory consumption has been reached
 */
class OffloadingOutputStream extends ThresholdingOutputStream
{

    // ----------------------------------------------------------- Data members

    /**
     * The output stream to which data will be written prior to the theshold
     * being reached.
     */
    private ByteArrayOutputStream memoryOutputStream;

    /**
     * The output stream to which data will be written at any given time. This
     * will always be one of <code>memoryOutputStream</code> or
     * <code>diskOutputStream</code>.
     */
    private OutputStream currentOutputStream;

    /**
     * The path to which output will be directed if the threshold is exceeded.
     */
    private Path outputPath = null;

    /**
     * The temporary file prefix.
     */
    private final String prefix;

    /**
     * The temporary file suffix.
     */
    private final String suffix;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs an instance of this class which will trigger an event at the
     * specified threshold, and save data to a temporary file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param prefix    Prefix to use for the temporary file.
     * @param suffix    Suffix to use for the temporary file.
     * @since 1.4
     */
    public OffloadingOutputStream( int threshold, String prefix, String suffix )
    {
        super( threshold );

        if ( prefix == null )
        {
            throw new IllegalArgumentException( "Temporary file prefix is missing" );
        }

        memoryOutputStream = new ByteArrayOutputStream( threshold / 10 );
        currentOutputStream = memoryOutputStream;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    // --------------------------------------- ThresholdingOutputStream methods

    /**
     * Returns the current output stream. This may be memory based or disk
     * based, depending on the current state with respect to the threshold.
     *
     * @return The underlying output stream.
     * @throws java.io.IOException if an error occurs.
     */
    @Override
    protected OutputStream getStream() throws IOException
    {
        return currentOutputStream;
    }

    /**
     * Switches the underlying output stream from a memory based stream to one
     * that is backed by disk. This is the point at which we realise that too
     * much data is being written to keep in memory, so we elect to switch to
     * disk-based storage.
     *
     * @throws java.io.IOException if an error occurs.
     */
    @Override
    protected void thresholdReached() throws IOException
    {
        outputPath = Files.createTempFile( prefix, suffix );
        currentOutputStream = Files.newOutputStream( outputPath );
    }

    public InputStream getInputStream() throws IOException
    {

        InputStream memoryAsInput = memoryOutputStream.toInputStream();
        if ( outputPath == null )
        {
            return memoryAsInput;
        }
        return new SequenceInputStream( memoryAsInput, Files.newInputStream( outputPath ) );
    }

    // --------------------------------------------------------- Public methods

    /**
     * Returns the data for this output stream as an array of bytes, assuming
     * that the data has been retained in memory. If the data was written to
     * disk, this method returns <code>null</code>.
     *
     * @return The data for this output stream, or <code>null</code> if no such
     * data is available.
     */
    public byte[] getData()
    {
        if ( memoryOutputStream != null )
        {
            return memoryOutputStream.toByteArray();
        }
        return null;
    }

    /**
     * Returns either the output file specified in the constructor or
     * the temporary file created or null.
     * <p>
     * If the constructor specifying the file is used then it returns that
     * same output file, even when threshold has not been reached.
     * <p>
     * If constructor specifying a temporary file prefix/suffix is used
     * then the temporary file created once the threshold is reached is returned
     * If the threshold was not reached then <code>null</code> is returned.
     *
     * @return The file for this output stream, or <code>null</code> if no such
     * file exists.
     */
    public File getFile()
    {
        return outputPath != null ? outputPath.toFile() : null;
    }

    /**
     * Closes underlying output stream.
     *
     * @throws java.io.IOException if an error occurs.
     */
    @Override
    public void close() throws IOException
    {
        super.close();
        currentOutputStream.close();
    }
}
