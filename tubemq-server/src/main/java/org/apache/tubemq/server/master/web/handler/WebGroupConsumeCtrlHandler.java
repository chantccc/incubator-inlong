/*
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

package org.apache.tubemq.server.master.web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.tubemq.server.common.utils.ProcessResult;
import org.apache.tubemq.server.common.utils.WebParameterUtils;
import org.apache.tubemq.server.master.TMaster;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;




public class WebGroupConsumeCtrlHandler extends AbstractWebHandler {

    public WebGroupConsumeCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_group_csmctrl_info",
                "adminQueryGroupConsumeCtrlInfo");
        // register modify method
        registerModifyWebMethod("admin_add_group_csmctrl_info",
                "adminAddGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_batch_add_group_csmctrl_info",
                "adminBatchAddGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_update_group_csmctrl_info",
                "adminModGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_batch_update_group_csmctrl_info",
                "adminBatchModGroupConsumeCtrlInfo");
        registerModifyWebMethod("admin_delete_group_csmctrl_info",
                "adminDelGroupConsumeCtrlInfo");
    }


    /**
     * query group consume control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminQueryGroupConsumeCtrlInfo(HttpServletRequest req,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get consumeEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.CONSUMEENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean consumeEnable = (Boolean) result.getRetData();
        // get filterEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.FILTERENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean filterEnable = (Boolean) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondSet(req, false, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> filterCondSet = (Set<String>) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                consumeEnable, null, filterEnable, null);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                metaDataManager.getGroupConsumeCtrlConf(groupSet, topicNameSet, qryEntity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> consumeCtrlEntityList : qryResultMap.values()) {
            if (consumeCtrlEntityList == null || consumeCtrlEntityList.isEmpty()) {
                continue;
            }
            for (GroupConsumeCtrlEntity entity : consumeCtrlEntityList) {
                if (entity == null
                        || !WebParameterUtils.isFilterSetFullIncluded(
                                filterCondSet, entity.getFilterCondStr())) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                entity.toWebJsonStr(sBuffer, true, true);
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * add group consume control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminAddGroupConsumeCtrlInfo(HttpServletRequest req,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        return innAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, true);
    }

    /**
     * Add group consume control info in batch
     *
     * @param req
     * @return
     */
    public StringBuilder adminBatchAddGroupConsumeCtrlInfo(HttpServletRequest req,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        return innBatchAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, true);
    }

    /**
     * modify group consume control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminModGroupConsumeCtrlInfo(HttpServletRequest req,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        return innAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, false);
    }

    /**
     * Modify group consume control info in batch
     *
     * @param req
     * @return
     */
    public StringBuilder adminBatchModGroupConsumeCtrlInfo(HttpServletRequest req,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        return innBatchAddOrUpdGroupConsumeCtrlInfo(req, sBuffer, result, false);
    }

    /**
     * Delete group consume configure info
     *
     * @param req
     * @return
     */
    public StringBuilder adminDelGroupConsumeCtrlInfo(HttpServletRequest req,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get groupName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // execute delete operation
        List<GroupProcessResult> retInfo =
                metaDataManager.delGroupConsumeCtrlConf(opEntity.getModifyUser(),
                        groupNameSet, topicNameSet, sBuffer, result);
        buildRetInfo(retInfo, sBuffer);
        return sBuffer;
    }

    private StringBuilder innAddOrUpdGroupConsumeCtrlInfo(HttpServletRequest req,
                                                          StringBuilder sBuffer,
                                                          ProcessResult result,
                                                          boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opInfoEntity = (BaseEntity) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                metaDataManager, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // get groupName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get consumeEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.CONSUMEENABLE, false,
                (isAddOp ? true : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean consumeEnable = (Boolean) result.getRetData();
        // get disableReason list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.REASON, false,
                (isAddOp ? "" : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String disableRsn = (String) result.getRetData();
        // get filterEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.FILTERENABLE, false,
                (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean filterEnable = (Boolean) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondString(req, false, isAddOp, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String filterCondStr = (String) result.getRetData();
        // add group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfo.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(isAddOp,
                        opInfoEntity, groupName, topicName, consumeEnable, disableRsn,
                        filterEnable, filterCondStr, sBuffer, result));
            }
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innBatchAddOrUpdGroupConsumeCtrlInfo(HttpServletRequest req,
                                                               StringBuilder sBuffer,
                                                               ProcessResult result,
                                                               boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // check and get groupCsmJsonSet data
        if (!getGroupConsumeJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> batchAddInfoMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add group resource record
        GroupProcessResult addResult;
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (GroupConsumeCtrlEntity ctrlEntity : batchAddInfoMap.values()) {
            retInfo.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(
                    isAddOp, ctrlEntity, sBuffer, result));
        }
        buildRetInfo(retInfo, sBuffer);
        return sBuffer;
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
                                       StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupProcessResult result : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(result.getGroupName()).append("\"")
                    .append(",\"topicName\":\"").append(result.getTopicName()).append("\"")
                    .append(",\"success\":").append(result.isSuccess())
                    .append(",\"errCode\":").append(result.getErrCode())
                    .append(",\"errInfo\":\"").append(result.getErrInfo()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    private boolean getGroupConsumeJsonSetInfo(HttpServletRequest req, boolean isAddOp,
                                               BaseEntity defOpEntity, StringBuilder sBuffer,
                                               ProcessResult result) {
        // get groupCsmJsonSet field info
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPCSMJSONSET, true, null, result)) {
            return result.success;
        }
        List<Map<String, String>> filterJsonArray =
                (List<Map<String, String>>) result.getRetData();
        // parse groupCsmJsonSet field info
        GroupConsumeCtrlEntity itemConf;
        Map<String, String> itemsMap;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                metaDataManager.getTotalConfiguredTopicNames();
        for (int j = 0; j < filterJsonArray.size(); j++) {
            itemsMap = filterJsonArray.get(j);
            if (!WebParameterUtils.getStringParamValue(itemsMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.success;
            }
            String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemsMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.success;
            }
            String topicName = (String) result.getRetData();
            if (!configuredTopicSet.contains(topicName)) {
                result.setFailResult(sBuffer
                        .append(WebFieldDef.TOPICNAME.name)
                        .append(" ").append(topicName)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.success;
            }
            // get consumeEnable info
            if (!WebParameterUtils.getBooleanParamValue(itemsMap,
                    WebFieldDef.CONSUMEENABLE, false,
                    (isAddOp ? true : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean consumeEnable = (Boolean) result.getRetData();
            // get disableReason list
            if (!WebParameterUtils.getStringParamValue(itemsMap,
                    WebFieldDef.REASON, false, (isAddOp ? "" : null), sBuffer, result)) {
                return result.isSuccess();
            }
            String disableRsn = (String) result.getRetData();
            // get filterEnable info
            if (!WebParameterUtils.getBooleanParamValue(itemsMap,
                    WebFieldDef.FILTERENABLE, false,
                    (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean filterEnable = (Boolean) result.getRetData();
            // get filterConds info
            if (!WebParameterUtils.getFilterCondString(
                    itemsMap, false, isAddOp, sBuffer, result)) {
                return result.isSuccess();
            }
            String filterCondStr = (String) result.getRetData();
            // add record object
            itemConf = new GroupConsumeCtrlEntity(defOpEntity, groupName, topicName);
            itemConf.updModifyInfo(defOpEntity.getDataVerId(),
                    consumeEnable, disableRsn, filterEnable, filterCondStr);
            addRecordMap.put(itemConf.getRecordKey(), itemConf);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record in ")
                    .append(WebFieldDef.GROUPCSMJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

}