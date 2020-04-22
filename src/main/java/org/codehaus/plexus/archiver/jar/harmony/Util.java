/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.plexus.archiver.jar.harmony;

/**
 * Helpers for the archive module.
 */
public class Util {

    /**
     * Compares the given byte arrays and returns whether they are equal,
     * ignoring case differences and assuming they are ascii-encoded strings.
     * 
     * @param buf1
     *            first byte array to compare.
     * @param buf2
     *            second byte array to compare.
     * @return the result of the comparison.
     */
    public static boolean asciiEqualsIgnoreCase(byte[] buf1, byte[] buf2) {
        if (buf1 == null || buf2 == null) {
            return false;
        }
        if (buf1 == buf2) {
            return true;
        }
        if (buf1.length != buf2.length) {
            return false;
        }

        for (int i = 0; i < buf1.length; i++) {
            byte b1 = buf1[i];
            byte b2 = buf2[i];
            if (b1 != b2 && toASCIIUpperCase(b1) != toASCIIUpperCase(b2)) {
                return false;
            }
        }
        return true;
    }

    private static final byte toASCIIUpperCase(byte b) {
        if ('a' <= b && b <= 'z') {
            return (byte) (b - ('a' - 'A'));
        }
        return b;
    }
}
