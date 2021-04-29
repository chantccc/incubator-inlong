/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.server.master.metamanage.metastore.dao.entity;

import java.util.Date;
import java.util.Objects;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.corebase.utils.SettingValidUtils;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.server.common.statusdef.EnableStatus;
import org.apache.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;


/*
 * store the topic authenticate control setting
 *
 */
public class TopicCtrlEntity extends BaseEntity implements Cloneable {

    private String topicName = "";
    // topic id, require globally unique
    private int topicNameId = TBaseConstants.META_VALUE_UNDEFINED;
    private EnableStatus authCtrlStatus = EnableStatus.STATUS_UNDEFINE;
    private int maxMsgSizeInB = TBaseConstants.META_VALUE_UNDEFINED;
    private int maxMsgSizeInMB = TBaseConstants.META_VALUE_UNDEFINED;


    public TopicCtrlEntity() {
        super();
    }


    public TopicCtrlEntity(String topicName, int topicNameId,
                           int maxMsgSizeInMB, String createUser) {
        super(createUser, new Date());
        this.topicName = topicName;
        this.topicNameId = topicNameId;
        this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        this.maxMsgSizeInB =
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(maxMsgSizeInMB);
        this.maxMsgSizeInMB = maxMsgSizeInMB;
    }

    public TopicCtrlEntity(BaseEntity opEntity, String topicName) {
        super(opEntity);
        this.topicName = topicName;
    }

    public TopicCtrlEntity(String topicName, int topicNameId,
                           boolean enableAuth, int maxMsgSizeInB,
                           long dataVersionId, String createUser,
                           Date createDate, String modifyUser, Date modifyDate) {
        super(dataVersionId, createUser, createDate, modifyUser, modifyDate);
        this.topicName = topicName;
        this.topicNameId = topicNameId;
        this.fillMaxMsgSize(maxMsgSizeInB);
        if (enableAuth) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    public TopicCtrlEntity(BdbTopicAuthControlEntity bdbEntity) {
        super(bdbEntity.getDataVerId(),
                bdbEntity.getCreateUser(), bdbEntity.getCreateDate());
        this.topicName = bdbEntity.getTopicName();
        this.topicNameId = bdbEntity.getTopicId();
        this.fillMaxMsgSize(maxMsgSizeInB);
        if (bdbEntity.isEnableAuthControl()) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
        this.setAttributes(bdbEntity.getAttributes());
    }

    public BdbTopicAuthControlEntity buildBdbTopicAuthControlEntity() {
        BdbTopicAuthControlEntity bdbEntity =
                new BdbTopicAuthControlEntity(topicName, isAuthCtrlEnable(),
                        getAttributes(), getCreateUser(), getCreateDate());
        bdbEntity.setTopicId(topicNameId);
        bdbEntity.setDataVerId(getDataVerId());
        bdbEntity.setMaxMsgSize(maxMsgSizeInB);
        return bdbEntity;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isAuthCtrlEnable() {
        return authCtrlStatus == EnableStatus.STATUS_ENABLE;
    }

    public void setEnableAuthCtrl(boolean enableAuth) {
        if (enableAuth) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    public EnableStatus getAuthCtrlStatus() {
        return authCtrlStatus;
    }

    public void setAuthCtrlStatus(EnableStatus authCtrlStatus) {
        this.authCtrlStatus = authCtrlStatus;
    }

    public int getMaxMsgSizeInB() {
        return maxMsgSizeInB;
    }

    public int getMaxMsgSizeInMB() {
        return maxMsgSizeInMB;
    }

    public int getTopicId() {
        return topicNameId;
    }

    public void setTopicId(int topicNameId) {
        this.topicNameId = topicNameId;
    }

    /**
     * update subclass field values
     *
     * @return if changed
     */
    public boolean updModifyInfo(long dataVerId, int topicNameId,
                                 int newMaxMsgSizeMB, Boolean enableTopicAuth) {
        boolean changed = false;
        // check and set brokerPort info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        // check and set topicNameId info
        if (topicNameId != TBaseConstants.META_VALUE_UNDEFINED
                && this.topicNameId != topicNameId) {
            changed = true;
            this.topicNameId = topicNameId;
        }
        // check and set modified field
        if (newMaxMsgSizeMB != TBaseConstants.META_VALUE_UNDEFINED) {
            int newMaxMsgSizeB =
                    SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(newMaxMsgSizeMB);
            if (this.maxMsgSizeInB != newMaxMsgSizeB) {
                changed = true;
                this.maxMsgSizeInB = newMaxMsgSizeB;
                this.maxMsgSizeInMB = newMaxMsgSizeMB;
            }
        }
        // check and set authCtrlStatus info
        if (enableTopicAuth != null
                && this.authCtrlStatus.isEnable() != enableTopicAuth) {
            setEnableAuthCtrl(enableTopicAuth);
            changed = true;
        }
        if (changed) {
            updSerialId();
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   topicName, maxMsgSizeInB, authCtrlStatus
     * @return true: matched, false: not match
     */
    public boolean isMatched(TopicCtrlEntity target) {
        if (target == null) {
            return true;
        }
        if (!super.isMatched(target)) {
            return false;
        }
        if ((target.getMaxMsgSizeInB() != TBaseConstants.META_VALUE_UNDEFINED
                && target.getMaxMsgSizeInB() != this.maxMsgSizeInB)
                || (TStringUtils.isNotBlank(target.getTopicName())
                && !target.getTopicName().equals(this.topicName))
                || (target.getAuthCtrlStatus() != EnableStatus.STATUS_UNDEFINE
                && target.getAuthCtrlStatus() != this.authCtrlStatus)
                || (target.getTopicId() != TBaseConstants.META_VALUE_UNDEFINED
                && target.getTopicId() != this.topicNameId)) {
            return false;
        }
        return true;
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuilder,
                                      boolean isLongName,
                                      boolean fullFormat) {
        if (isLongName) {
            sBuilder.append("{\"topicName\":\"").append(topicName).append("\"")
                    .append(",\"topicNameId\":").append(topicNameId)
                    .append(",\"enableAuthControl\":").append(authCtrlStatus.isEnable())
                    .append(",\"maxMsgSizeInMB\":").append(maxMsgSizeInMB);
        } else {
            sBuilder.append("{\"topic\":\"").append(topicName).append("\"")
                    .append(",\"topicId\":").append(topicNameId)
                    .append(",\"acEn\":").append(authCtrlStatus.isEnable())
                    .append(",\"mxMsgInMB\":").append(maxMsgSizeInMB);
        }
        super.toWebJsonStr(sBuilder, isLongName);
        if (fullFormat) {
            sBuilder.append("}");
        }
        return sBuilder;
    }

    private void fillMaxMsgSize(int maxMsgSizeInB) {
        this.maxMsgSizeInB = maxMsgSizeInB;
        this.maxMsgSizeInMB =
                maxMsgSizeInB / TBaseConstants.META_MB_UNIT_SIZE;
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(TopicCtrlEntity other) {
        return topicNameId == other.topicNameId
                && maxMsgSizeInB == other.maxMsgSizeInB
                && topicName.equals(other.topicName)
                && authCtrlStatus == other.authCtrlStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicCtrlEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TopicCtrlEntity that = (TopicCtrlEntity) o;
        return isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), topicName,
                topicNameId, authCtrlStatus, maxMsgSizeInB);
    }

    @Override
    public TopicCtrlEntity clone() {
        TopicCtrlEntity copy = (TopicCtrlEntity) super.clone();
        copy.setAuthCtrlStatus(getAuthCtrlStatus());
        return copy;
    }
}