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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olivier Lamy
 */
class FilePermissionUtilsTest {

    Logger getLogger() {
        return LoggerFactory.getLogger("foo");
    }

    @ParameterizedTest
    @CsvSource({
        "200, true, true, false, true, false, true",
        "500, false, true, true, true, true, true",
        "700, true, true, true, true, true, true",
        "707, true, false, true, false, true, false"
    })
    void filePermissionFromMode(
            String mode,
            boolean writable,
            boolean ownerOnlyWritable,
            boolean executable,
            boolean ownerOnlyExecutable,
            boolean readable,
            boolean ownerOnlyReadable) {
        FilePermission fp = FilePermissionUtils.getFilePermissionFromMode(mode, getLogger());
        assertAll(
                () -> assertEquals(writable, fp.isWritable()),
                () -> assertEquals(ownerOnlyWritable, fp.isOwnerOnlyWritable()),
                () -> assertEquals(executable, fp.isExecutable()),
                () -> assertEquals(ownerOnlyExecutable, fp.isOwnerOnlyExecutable()),
                () -> assertEquals(readable, fp.isReadable()),
                () -> assertEquals(ownerOnlyReadable, fp.isOwnerOnlyReadable()));
    }
}
