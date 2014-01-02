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

package com.dinstone.zkclient.leader;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * @author guojf
 * @version 1.0.0.2013-10-17
 */
public class ZookeeperTest {

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        ZooKeeper zooKeeper = getZookeeper();
        String path = "/ddd";

        exist0(zooKeeper, path);
        createPath(zooKeeper, path);
        
        exist0(zooKeeper, path);
        createPath(zooKeeper, path);

        Thread.sleep(300000);
    }

    /**
     *
     */
    private static void exist0(ZooKeeper zooKeeper, String path) {
        try {
            Stat stat = zooKeeper.exists(path, true);
            if (stat == null) {
                System.out.println("path[" + path + "] is not exsited");
            } else {
                System.out.println("path[" + path + "] is exsited");
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param zooKeeper
     * @param path
     */
    private static void createPath(ZooKeeper zooKeeper, String path) {
        try {
            zooKeeper.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    private static ZooKeeper getZookeeper() {
        ZooKeeper zooKeeper = null;

        final CountDownLatch connectSingal = new CountDownLatch(1);
        try {
            String quorumServers = "172.21.31.32:2181";
            zooKeeper = new ZooKeeper(quorumServers, 1000, new Watcher() {

                public void process(WatchedEvent event) {
                    System.out.println("Received zookeeper event, type={" + event.getType() + "}, state={"
                            + event.getState() + "}, ={" + event.getPath() + "}");

                    if (KeeperState.SyncConnected == event.getState()) {
                        connectSingal.countDown();
                    } else if (KeeperState.Expired == event.getState()) {
                        System.err.println("Session is expired, need to redo the action");
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            connectSingal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return zooKeeper;
    }

}
