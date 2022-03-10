/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.GroupResCtrlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsGroupResCtrlMapperImpl implements GroupResCtrlMapper {
    protected static final Logger logger =
            LoggerFactory.getLogger(AbsGroupResCtrlMapperImpl.class);
    private final ConcurrentHashMap<String/* groupName */, GroupResCtrlEntity>
            groupBaseCtrlCache = new ConcurrentHashMap<>();

    public AbsGroupResCtrlMapperImpl() {
        // Initial instant
    }

    @Override
    public boolean addGroupResCtrlConf(GroupResCtrlEntity entity,
                                       StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity curEntity =
                groupBaseCtrlCache.get(entity.getGroupName());
        if (curEntity != null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    strBuff.append("The group control configure ").append(entity.getGroupName())
                            .append(" already exists, please delete it first!")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        if (putConfig2Persistent(entity, strBuff, result)) {
            groupBaseCtrlCache.put(entity.getGroupName(), entity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean updGroupResCtrlConf(GroupResCtrlEntity entity,
                                       StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity curEntity =
                groupBaseCtrlCache.get(entity.getGroupName());
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("The group control configure ").append(entity.getGroupName())
                            .append(" is not exists, please add it first!")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        if (curEntity.equals(entity)) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    strBuff.append("The group control configure ").append(entity.getGroupName())
                            .append(" have not changed, please confirm it first!")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        if (putConfig2Persistent(entity, strBuff, result)) {
            groupBaseCtrlCache.put(entity.getGroupName(), entity);
            result.setSuccResult(curEntity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean delGroupResCtrlConf(String groupName, StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity curEntity =
                groupBaseCtrlCache.get(groupName);
        if (curEntity == null) {
            result.setSuccResult(null);
            return true;
        }
        delConfigFromPersistent(groupName, strBuff);
        groupBaseCtrlCache.remove(groupName);
        result.setSuccResult(curEntity);
        return true;
    }

    @Override
    public GroupResCtrlEntity getGroupResCtrlConf(String groupName) {
        return groupBaseCtrlCache.get(groupName);
    }

    @Override
    public Map<String, GroupResCtrlEntity> getGroupResCtrlConf(Set<String> groupNameSet,
                                                               GroupResCtrlEntity qryEntry) {
        Map<String, GroupResCtrlEntity> retMap = new HashMap<>();
        if (groupNameSet == null || groupNameSet.isEmpty()) {
            for (GroupResCtrlEntity entry : groupBaseCtrlCache.values()) {
                if (entry == null || (qryEntry != null && !entry.isMatched(qryEntry))) {
                    continue;
                }
                retMap.put(entry.getGroupName(), entry);
            }
        } else {
            GroupResCtrlEntity entry;
            for (String groupName : groupNameSet) {
                entry = groupBaseCtrlCache.get(groupName);
                if (entry == null || (qryEntry != null && !entry.isMatched(qryEntry))) {
                    continue;
                }
                retMap.put(entry.getGroupName(), entry);
            }
        }
        return retMap;
    }

    /**
     * Clear cached data
     */
    protected void clearCachedData() {
        groupBaseCtrlCache.clear();
    }

    /**
     * Add or update a record
     *
     * @param entity  need added or updated entity
     */
    protected void addOrUpdCacheRecord(GroupResCtrlEntity entity) {
        groupBaseCtrlCache.put(entity.getGroupName(), entity);
    }

    /**
     * Put group control configure information into persistent store
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result process result with old value
     * @return the process result
     */
    protected abstract boolean putConfig2Persistent(GroupResCtrlEntity entity,
                                                    StringBuilder strBuff, ProcessResult result);

    /**
     * Delete group control configure information from persistent storage
     *
     * @param recordKey  the record key
     * @param strBuff    the string buffer
     * @return the process result
     */
    protected abstract boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff);
}