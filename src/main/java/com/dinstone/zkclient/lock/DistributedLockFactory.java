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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.dinstone.zkclient.core.ZkConnection;
import com.dinstone.zkclient.core.ZkException;

/**
 * @author guojf
 * @version 1.0.0.2013-12-19
 */
public class DistributedLockFactory {

    private ZkConnection zkConnection;

    private String rootPath = "/lock";

    /**
     * 
     */
    public DistributedLockFactory(String zkServers, int sessionTimeout) {
        this.zkConnection = new ZkConnection(zkServers, sessionTimeout);
    }

    public DistributedLock createLock(String resourceTag) throws InterruptedException {
        String resourcePath = rootPath + "/" + resourceTag;
        try {
            ZooKeeper zookeeper = this.zkConnection.getZooKeeper();
            createPath(zookeeper, resourcePath, null, null);
        } catch (KeeperException e) {
            throw new ZkException(e);
        }

        return new ZkDistributedLock(this.zkConnection, resourcePath);
    }

    private void createPath(ZooKeeper zookeeper, String path, byte[] data, Watcher w) throws KeeperException,
            InterruptedException {
        Stat stat = zookeeper.exists(path, w);
        if (stat == null) {
            int index = path.lastIndexOf('/');
            if (index > 0) {
                String parent = path.substring(0, index);
                createPath(zookeeper, parent, null, null);
            }

            try {
                zookeeper.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (NodeExistsException e) {
                // ignore
            }
        }
    }

}
