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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Post Java 21 implementation. Create one Virtual Thread per task execution. Apply same thread names as well.
 *
 * @since 4.10.1
 */
public class ConcurrentJarCreatorExecutorServiceFactory {
    private static final AtomicInteger POOL_COUNTER = new AtomicInteger();

    static ExecutorService createExecutorService(int poolSize) {
        int poolCount = POOL_COUNTER.incrementAndGet();
        AtomicInteger threadCounter = new AtomicInteger();
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("plx-arch-" + poolCount + "-" + threadCounter.incrementAndGet()).factory());
    }
}
