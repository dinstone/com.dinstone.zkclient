/*
 * Copyright (C) 2012~2013 dinstone<dinstone@163.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dinstone.zkclient.lock;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author guojf
 * @version 1.0.0.2013-12-20
 */
public class DistributedLockTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link com.dinstone.zkclient.lock.DistributedLock#lock()}
     * .
     * 
     * @throws InterruptedException
     */
    @Test
    public void testLock() throws InterruptedException {
        int count = 2;
        String node = "Node1";

        String zkServers = "172.17.20.210:2181,172.17.20.211:2181,172.17.20.212:2181";
        DistributedLockFactory factory = new DistributedLockFactory(zkServers, 3000);
        CountDownLatch cdl = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(new Process(node, i, cdl, factory), "Process-" + i);
            t.start();
        }

        cdl.await();
    }

    static class Process implements Runnable {

        private String node;

        private CountDownLatch cdl;

        private int thread;

        private DistributedLockFactory factory;

        public Process(String node, int thread, CountDownLatch cdl, DistributedLockFactory factory) {
            this.node = node;
            this.thread = thread;
            this.cdl = cdl;
            this.factory = factory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                DistributedLock rlock = factory.createLock("rt");
                rlock.lock();
                try {
                    System.out.println("Node[" + node + "] thread[" + thread + "] acquire lock");

                    // do something
                    Thread.sleep(30000);
                } finally {
                    rlock.unlock();
                }

                System.out.println("Node[" + node + "] thread[" + thread + "] release lock");
            } catch (InterruptedException e) {
            } finally {
                cdl.countDown();
            }
        }

    }
}
