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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentJarCreatorExecutorServiceFactory {
    private static final AtomicInteger POOL_COUNTER = new AtomicInteger();

    static ExecutorService createExecutorService(int poolSize) {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        int poolCount = POOL_COUNTER.incrementAndGet();
        AtomicInteger threadCounter = new AtomicInteger();
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread =
                        new Thread(threadGroup, r, "plx-arch-" + poolCount + "-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        return new ThreadPoolExecutor(1, poolSize, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory);
    }
}
