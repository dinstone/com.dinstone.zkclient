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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author guojf
 * @version 1.0.0.2013-12-30
 */
public class LocalLockTest {

    public static void main(String[] args) {
        CountDownLatch end = new CountDownLatch(2);
        Lock lock = new ReentrantLock();
        for (int i = 0; i < 2; i++) {
            Thread t = new Thread(new Process(i, lock, end));
            t.start();
        }

        try {
            end.await();
            System.out.println("end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class Process implements Runnable {

        private Lock lock;

        private int index;

        private CountDownLatch end;

        /**
         * @param i
         * @param lock
         * @param end
         */
        public Process(int i, Lock lock, CountDownLatch end) {
            this.index = i;
            this.lock = lock;
            this.end = end;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            lock.lock();
            try {
                if (index % 2 == 0) {
                    return;
                }
                lock.unlock();
            } finally {
                end.countDown();
            }
        }

    }

}
