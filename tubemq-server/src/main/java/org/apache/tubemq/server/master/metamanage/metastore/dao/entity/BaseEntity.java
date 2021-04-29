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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.server.common.TServerConstants;
import org.apache.tubemq.server.common.utils.WebParameterUtils;


// AbstractEntity: entity's abstract class
public class BaseEntity implements Serializable, Cloneable {

    private long dataVersionId =
            TServerConstants.DEFAULT_DATA_VERSION;    // 0: default version， other: version
    private long serialId = TBaseConstants.META_VALUE_UNDEFINED;
    private String createUser = "";        // create user
    private Date createDate = null;        // create date
    private String modifyUser = "";       // modify user
    private Date modifyDate = null;        // modify date
    private String attributes = "";        // attribute info
    private String createDateStr = "";     // create data string
    private String modifyDateStr = "";     // create data string

    public BaseEntity() {

    }

    public BaseEntity(long dataVersionId) {
        this.dataVersionId = dataVersionId;
    }

    public BaseEntity(String createUser, Date createDate) {
        this(TServerConstants.DEFAULT_DATA_VERSION,
                createUser, createDate, createUser, createDate);
    }

    public BaseEntity(BaseEntity other) {
        this.dataVersionId = other.dataVersionId;
        this.createUser = other.createUser;
        this.setCreateDate(other.createDate);
        this.modifyUser = other.modifyUser;
        this.setModifyDate(other.modifyDate);
    }

    public BaseEntity(long dataVersionId, String createUser, Date createDate) {
        this(dataVersionId, createUser, createDate, createUser, createDate);
    }

    public BaseEntity(String createUser, Date createDate,
                      String modifyUser, Date modifyDate) {
        this(TServerConstants.DEFAULT_DATA_VERSION,
                createUser, createDate, modifyUser, modifyDate);
    }

    public BaseEntity(long dataVersionId,
                      String createUser, Date createDate,
                      String modifyUser, Date modifyDate) {
        this.dataVersionId = dataVersionId;
        this.createUser = createUser;
        this.setCreateDate(createDate);
        this.modifyUser = modifyUser;
        this.setModifyDate(modifyDate);
    }

    public boolean updBaseModifyInfo(BaseEntity opInfoEntity) {
        boolean changed = false;
        if (TStringUtils.isNotBlank(opInfoEntity.getModifyUser())
                && !Objects.equals(modifyUser, opInfoEntity.getModifyUser())) {
            changed = true;
            this.modifyUser = opInfoEntity.getModifyUser();
        }
        if (opInfoEntity.getModifyDate() != null
                && !Objects.equals(modifyDate, opInfoEntity.getModifyDate())) {
            changed = true;
            this.setModifyDate(opInfoEntity.getModifyDate());
        }
        if (TStringUtils.isNotBlank(opInfoEntity.getAttributes())
                && !Objects.equals(attributes, opInfoEntity.getAttributes())) {
            changed = true;
            this.attributes = opInfoEntity.getAttributes();
        }
        return changed;
    }

    public boolean updQueryKeyInfo(long newDataVerId,
                                   String newCreateUser,
                                   String newModifyUser) {
        boolean changed = false;
        // check and set dataVersionId field
        if (newDataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.dataVersionId != newDataVerId) {
            changed = true;
            this.dataVersionId = newDataVerId;
        }
        if (TStringUtils.isNotBlank(newCreateUser)
                && !Objects.equals(createUser, newCreateUser)) {
            changed = true;
            this.createUser = newCreateUser;
        }
        if (TStringUtils.isNotBlank(newModifyUser)
                && !Objects.equals(modifyUser, newModifyUser)) {
            changed = true;
            this.modifyUser = newModifyUser;
        }
        return changed;
    }

    public boolean updBaseModifyInfo(long newDataVerId, String newCreateUser,
                                     Date newCreateDate, String newModifyUser,
                                     Date newModifyDate, String newAttributes) {
        boolean changed = false;
        // check and set dataVersionId field
        if (newDataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.dataVersionId != newDataVerId) {
            changed = true;
            this.dataVersionId = newDataVerId;
        }
        if (TStringUtils.isNotBlank(newCreateUser)
                && !Objects.equals(createUser, newCreateUser)) {
            changed = true;
            this.createUser = newCreateUser;
        }
        if (newCreateDate != null
                && !Objects.equals(createDate, newCreateDate)) {
            changed = true;
            this.setCreateDate(newCreateDate);
        }
        if (TStringUtils.isNotBlank(newModifyUser)
                && !Objects.equals(modifyUser, newModifyUser)) {
            changed = true;
            this.modifyUser = newModifyUser;
        }
        if (newModifyDate != null
                && !Objects.equals(modifyDate, newModifyDate)) {
            changed = true;
            this.setModifyDate(newModifyDate);
        }
        if (TStringUtils.isNotBlank(newAttributes)
                && !Objects.equals(attributes, newAttributes)) {
            changed = true;
            this.attributes = newAttributes;
        }
        return changed;
    }

    public void setDataVersionId(long dataVersionId) {
        this.dataVersionId = dataVersionId;
    }

    public void setKeyAndVal(String key, String value) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes, key, value);
    }

    public String getValueByKey(String key) {
        return TStringUtils.getAttrValFrmAttributes(this.attributes, key);
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getCreateUser() {
        return createUser;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public long getDataVerId() {
        return dataVersionId;
    }

    public long getSerialId() {
        return serialId;
    }

    protected void updSerialId() {
        this.serialId = System.currentTimeMillis();
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public String getCreateDateStr() {
        return createDateStr;
    }

    public String getModifyDateStr() {
        return modifyDateStr;
    }

    public String toJsonString(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return toJsonString(gson);
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   dataVersionId, createUser, modifyUser
     * @return true: matched, false: not match
     */
    public boolean isMatched(BaseEntity target) {
        if (target == null) {
            return true;
        }
        if ((target.getDataVerId() != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != target.getDataVerId())
                || (TStringUtils.isNotBlank(target.getCreateUser())
                && !target.getCreateUser().equals(createUser))
                || (TStringUtils.isNotBlank(target.getModifyUser())
                && !target.getModifyUser().equals(modifyUser))) {
            return false;
        }
        return true;
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @return
     */
    StringBuilder toWebJsonStr(StringBuilder sBuilder, boolean isLongName) {
        if (isLongName) {
            sBuilder.append(",\"dataVersionId\":").append(dataVersionId)
                    .append(",\"serialId\":").append(serialId)
                    .append(",\"createUser\":\"").append(createUser).append("\"")
                    .append(",\"createDate\":\"").append(createDateStr).append("\"")
                    .append(",\"modifyUser\":\"").append(modifyUser).append("\"")
                    .append(",\"modifyDate\":\"").append(modifyDateStr).append("\"")
                    .append(",\"attributes\":\"").append(attributes).append("\"");
        } else {
            sBuilder.append(",\"dVerId\":").append(dataVersionId)
                    .append(",\"cur\":\"").append(createUser).append("\"")
                    .append(",\"cDate\":\"").append(createDateStr).append("\"")
                    .append(",\"mur\":\"").append(modifyUser).append("\"")
                    .append(",\"mDate\":\"").append(modifyDateStr).append("\"")
                    .append(",\"attrs\":\"").append(attributes).append("\"");
        }
        return sBuilder;
    }

    private void setModifyDate(Date date) {
        this.modifyDate = date;
        this.modifyDateStr = WebParameterUtils.date2yyyyMMddHHmmss(date);
    }

    private void setCreateDate(Date date) {
        this.createDate = date;
        this.createDateStr = WebParameterUtils.date2yyyyMMddHHmmss(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity)) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return dataVersionId == that.dataVersionId &&
                serialId == that.serialId &&
                Objects.equals(createUser, that.createUser) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(modifyUser, that.modifyUser) &&
                Objects.equals(modifyDate, that.modifyDate) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataVersionId, serialId, createUser,
                createDate, modifyUser, modifyDate, attributes);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Throwable e) {
            return null;
        }
    }
}