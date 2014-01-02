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

/**
 * @author guojf
 * @version 1.0.0.2013-12-19
 */
public class Proposal implements Comparable<Proposal> {

    private Integer code;

    private String path;

    private Object data;

    public Proposal(Integer code, String path, Object data) {
        this.code = code;
        this.path = path;
        this.data = data;
    }

    /**
     * the code to get
     * 
     * @return the code
     * @see Proposal#code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * the path to get
     * 
     * @return the path
     * @see Proposal#path
     */
    public String getPath() {
        return path;
    }

    /**
     * the data to get
     * 
     * @return the data
     * @see Proposal#data
     */
    public Object getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Proposal other = (Proposal) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Proposal [code=" + code + ", path=" + path + ", data=" + data + "]";
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Proposal other) {
        return this.getCode().compareTo(other.getCode());
    }

}
