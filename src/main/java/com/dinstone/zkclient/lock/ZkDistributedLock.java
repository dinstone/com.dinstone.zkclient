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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.zkclient.core.ZkConnection;
import com.dinstone.zkclient.core.ZkException;
import com.dinstone.zkclient.core.ZkStateListener;

/**
 * @author guojf
 * @version 1.0.0.2013-12-19
 */
public class ZkDistributedLock implements DistributedLock, ZkStateListener {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedLock.class);

    private static enum EventType {
        Retry, Expired, Locked
    }

    private ZkConnection zkConnection;

    private String resourcePath;

    private Proposal proposal;

    private ReentrantLock localLock = new ReentrantLock();

    private BlockingQueue<EventType> eventQueue = new LinkedBlockingQueue<EventType>();

    public ZkDistributedLock(ZkConnection zkConnection, String resourcePath) {
        this.zkConnection = zkConnection;
        this.resourcePath = resourcePath;
    }

    public void lock() throws InterruptedException {
        // check local lock
        localLock.lock();

        // check distributed lock
        if (!isLocked()) {
            do {
                try {
                    regist();
                    doLock();
                } catch (KeeperException e) {
                    throw new ZkException(e);
                }

                Object event = eventQueue.take();
                if (event == EventType.Expired) {
                    throw new ZkException("zookeeper session is expired");
                }
                if (event == EventType.Locked) {
                    break;
                }
            } while (true);
        }
    }

    public void unlock() throws InterruptedException {
        try {
            if (isLocked()) {
                try {
                    getZooKeeper().delete(proposal.getPath(), -1);
                } finally {
                    proposal = null;
                }
            }
        } catch (KeeperException e) {
            // ignore handle exception
            LOG.warn("unhandle exception", e);
        } finally {
            zkConnection.removeStateListener(this);
            localLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.dinstone.zkclient.core.ZkStateListener#onStateChanged(org.apache.zookeeper.Watcher.Event.KeeperState)
     */
    public void onStateChanged(KeeperState state) {
        if (KeeperState.Expired == state) {
            eventQueue.add(EventType.Expired);
        }
    }

    private ZooKeeper getZooKeeper() throws InterruptedException {
        return zkConnection.getZooKeeper();
    }

    private void doLock() throws KeeperException, InterruptedException {
        SortedSet<Proposal> offerSet = getProposals();
        Proposal[] offers = offerSet.toArray(new Proposal[offerSet.size()]);
        for (int i = 0; i < offers.length; i++) {
            Proposal leaderOffer = offers[i];
            if (this.proposal.equals(leaderOffer)) {
                if (i == 0) {
                    // we have lock
                    eventQueue.add(EventType.Locked);
                } else {
                    // find previous leader's offer and watch it
                    watch(offers[i - 1]);
                }

                break;
            }
        }
    }

    private void watch(Proposal prevOffer) throws KeeperException, InterruptedException {
        Stat stat = getZooKeeper().exists(prevOffer.getPath(), new Watcher() {

            public void process(WatchedEvent event) {
                if (event.getType().equals(Watcher.Event.EventType.NodeDeleted)) {
                    if (!event.getPath().equals(ZkDistributedLock.this.proposal.getPath())) {
                        eventQueue.add(EventType.Retry);
                    }
                }
            }

        });

        if (stat == null) {
            doLock();
        }
    }

    private void regist() throws KeeperException, InterruptedException {
        if (proposal == null) {
            // regist offer
            String offerPrefix = "offer_";
            String offerPath = resourcePath + "/" + offerPrefix;

            String hosts = getHostAddress();
            byte[] cdata = hosts.getBytes(Charset.forName("utf-8"));
            String offer = getZooKeeper()
                .create(offerPath, cdata, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

            // create leader offer
            Integer code = Integer.valueOf(offer.substring(offer.lastIndexOf("_") + 1));
            proposal = new Proposal(code, offer, hosts);

            zkConnection.addStateListener(this);
        }
    }

    private String getHostAddress() {
        StringBuilder hostAdd = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> ads = ni.getInetAddresses();
                while (ads.hasMoreElements()) {
                    java.net.InetAddress ipAdd = ads.nextElement();
                    if (ipAdd.isSiteLocalAddress() && !ipAdd.isLoopbackAddress() && (ipAdd instanceof Inet4Address)) {
                        hostAdd.append(ipAdd);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return hostAdd.toString();
    }

    private boolean isLocked() throws InterruptedException {
        if (proposal == null) {
            return false;
        }

        try {
            SortedSet<Proposal> offers = getProposals();
            if (offers.size() > 0) {
                Proposal first = offers.first();
                if (proposal.equals(first)) {
                    return true;
                }
            }
        } catch (KeeperException e) {
            throw new ZkException(e);
        }

        return false;
    }

    private SortedSet<Proposal> getProposals() throws KeeperException, InterruptedException {
        List<String> offerNames = getZooKeeper().getChildren(resourcePath, null);
        SortedSet<Proposal> offers = new TreeSet<Proposal>();
        for (String offerName : offerNames) {
            Integer code = Integer.valueOf(offerName.substring(offerName.lastIndexOf("_") + 1));
            String path = this.resourcePath + "/" + offerName;
            offers.add(new Proposal(code, path, null));
        }

        return offers;
    }

}
