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

package com.dinstone.zkclient.core;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author guojf
 * @version 1.0.0.2013-12-27
 */
public class ZkConnection {

    private static final Logger LOG = LoggerFactory.getLogger(ZkConnection.class);

    private String zkServers;

    private int timeout;

    private ZooKeeper zooKeeper;

    private final Set<ZkStateListener> stateListeners = new CopyOnWriteArraySet<ZkStateListener>();

    /**
     * @param zkServers
     * @param timeout
     */
    public ZkConnection(String zkServers, int timeout) {
        this.zkServers = zkServers;
        this.timeout = timeout;
    }

    public synchronized ZooKeeper getZooKeeper() throws InterruptedException {
        if (this.zooKeeper == null || !zooKeeper.getState().isAlive()) {
            final CountDownLatch connectSingal = new CountDownLatch(1);
            try {
                this.zooKeeper = new ZooKeeper(zkServers, timeout, new Watcher() {

                    public void process(WatchedEvent event) {
                        LOG.debug("Received zookeeper event, {}", event.toString());
                        if (event.getPath() == null) {
                            if (KeeperState.SyncConnected == event.getState()) {
                                connectSingal.countDown();
                                fireStateChangedEvent(event.getState());
                            } else if (KeeperState.Disconnected == event.getState()) {
                                fireStateChangedEvent(event.getState());
                            } else if (KeeperState.Expired == event.getState()) {
                                LOG.debug("Session is expired, need to clear process");
                                fireStateChangedEvent(event.getState());
                            }
                        }
                    }
                });
            } catch (IOException e) {
                throw new ZkException("connect zookeeper error", e);
            }
            connectSingal.await();
        }

        return this.zooKeeper;
    }

    private void fireStateChangedEvent(KeeperState state) {
        for (ZkStateListener listener : stateListeners) {
            listener.onStateChanged(state);
        }
    }

    public boolean addStateListener(ZkStateListener listener) {
        return this.stateListeners.add(listener);
    }

    public boolean removeStateListener(ZkStateListener listener) {
        return this.stateListeners.remove(listener);
    }

}
