/*
 * Copyright The Plexus developers.
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
package org.codehaus.plexus.archiver.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceUtilsTest {

    public static Stream<Arguments> testIsUpToDate() {
        return Stream.of(
                Arguments.of(0, 0, false), // dest and src 0
                Arguments.of(100, 0, false), // dest 0
                Arguments.of(0, 100, false), // src 0
                Arguments.of(100, 200, true),
                Arguments.of(200, 100, false),
                Arguments.of(100, 100, true));
    }

    @ParameterizedTest
    @MethodSource
    void testIsUpToDate(long source, long destination, boolean expected) {
        assertThat(ResourceUtils.isUptodate(source, destination)).isEqualTo(expected);
    }
}
