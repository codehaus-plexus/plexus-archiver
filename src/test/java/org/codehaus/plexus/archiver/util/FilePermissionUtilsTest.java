/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.plexus.archiver.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 */
public class FilePermissionUtilsTest
{

    Logger getLogger()
    {
        return LoggerFactory.getLogger( "foo" );
    }

    @Test
    public void testOnlyWritableOnlyUser() throws Exception
    {
        FilePermission fp = FilePermissionUtils.getFilePermissionFromMode( "200", getLogger() );
        assertTrue( fp.isWritable() );
        assertTrue( fp.isOwnerOnlyWritable() );
        assertFalse( fp.isExecutable() );
        assertTrue( fp.isOwnerOnlyExecutable() );
        assertFalse( fp.isReadable() );
    }

    @Test
    public void testExecAndRead()
    {
        FilePermission fp = FilePermissionUtils.getFilePermissionFromMode( "500", getLogger() );
        assertFalse( fp.isWritable() );
        assertTrue( fp.isOwnerOnlyWritable() );
        assertTrue( fp.isExecutable() );
        assertTrue( fp.isOwnerOnlyExecutable() );
        assertTrue( fp.isReadable() );
    }

    @Test
    public void testAllUser()
    {
        FilePermission fp = FilePermissionUtils.getFilePermissionFromMode( "700", getLogger() );
        assertTrue( fp.isWritable() );
        assertTrue( fp.isOwnerOnlyWritable() );
        assertTrue( fp.isExecutable() );
        assertTrue( fp.isOwnerOnlyExecutable() );
        assertTrue( fp.isReadable() );
    }

    @Test
    public void testAllAllUser()
    {
        FilePermission fp = FilePermissionUtils.getFilePermissionFromMode( "707", getLogger() );
        assertTrue( fp.isWritable() );
        assertFalse( fp.isOwnerOnlyWritable() );
        assertTrue( fp.isExecutable() );
        assertFalse( fp.isOwnerOnlyExecutable() );
        assertTrue( fp.isReadable() );
        assertFalse( fp.isOwnerOnlyReadable() );
    }

}
