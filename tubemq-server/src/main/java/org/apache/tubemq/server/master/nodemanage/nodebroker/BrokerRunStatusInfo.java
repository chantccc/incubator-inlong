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

package org.apache.tubemq.server.master.nodemanage.nodebroker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.corebase.cluster.BrokerInfo;
import org.apache.tubemq.corebase.cluster.TopicInfo;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.corebase.utils.Tuple2;
import org.apache.tubemq.corebase.utils.Tuple3;
import org.apache.tubemq.corebase.utils.Tuple4;
import org.apache.tubemq.server.common.TServerConstants;
import org.apache.tubemq.server.common.statusdef.ManageStatus;
import org.apache.tubemq.server.common.statusdef.StepStatus;
import org.apache.tubemq.server.master.utils.BrokerStatusSamplePrint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BrokerRunStatusInfo {

    private static final Logger logger =
            LoggerFactory.getLogger(BrokerRunStatusInfo.class);
    private static final BrokerStatusSamplePrint statusFsmSamplePrint =
            new BrokerStatusSamplePrint(logger);

    private final BrokerRunManager brokerRunManager;

    private BrokerInfo brokerInfo;
    private long createId;
    // config change flag
    private AtomicBoolean isConfChanged = new AtomicBoolean(false);
    private AtomicLong confChangeNo =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    // data sync status
    private StepStatus curStepStatus;
    private volatile long nextStepOpTimeInMills = 0;
    // current loaded change No.
    private AtomicLong confLoadedNo =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    // broker sync data info
    BrokerSyncData brokerSyncData = new BrokerSyncData();
    // broker status conditions
    private boolean isOnline = false;     // broker online flag
    private boolean isDoneDataLoad = false;
    private boolean isDoneDataSub = false;
    private boolean isDoneDataPub = false;
    private boolean isOverTLS = false;    // enable tls
    private long lastBrokerSyncTime = 0;
    private long maxConfLoadedTimeInMs = 0;
    private long curConfLoadTimeInMs = 0;



    public BrokerRunStatusInfo(BrokerRunManager brokerRunManager, BrokerInfo brokerInfo,
                               ManageStatus mngStatus, String brokerConfInfo,
                               Map<String, String> topicConfInfoMap, boolean isOverTls) {
        this.brokerRunManager = brokerRunManager;
        reInitRunStatusInfo(brokerInfo, mngStatus,
                brokerConfInfo, topicConfInfoMap, isOverTls);
    }

    public void reInitRunStatusInfo(BrokerInfo brokerInfo, ManageStatus mngStatus,
                                    String brokerConfInfo, Map<String, String> topicConfInfoMap,
                                    boolean isOverTls) {
        resetStatusInfo();
        this.createId = System.nanoTime();
        this.brokerInfo = brokerInfo;
        long curTime = System.currentTimeMillis();
        this.isOverTLS = isOverTls;
        this.isConfChanged.set(true);
        this.confChangeNo.set(curTime);
        this.brokerSyncData.updBrokerSyncData(true,
                confChangeNo.get(), mngStatus, brokerConfInfo, topicConfInfoMap);
        this.curStepStatus = StepStatus.STEP_STATUS_WAIT_ONLINE;
        this.nextStepOpTimeInMills = curTime + curStepStatus.getDelayDurInMs();
    }

    public void notifyDataChanged() {
        this.isConfChanged.set(true);
        this.confChangeNo.incrementAndGet();
    }

    public Tuple2<Boolean, Boolean> getDataSyncStatus() {
        return new Tuple2<>(this.isConfChanged.get(),
                this.confChangeNo.get() == this.confLoadedNo.get());
    }

    private boolean isDataChanged() {
        return (this.isConfChanged.get()
                || (this.curStepStatus == StepStatus.STEP_STATUS_UNDEFINED
                && this.confChangeNo.get() != this.confLoadedNo.get()));
    }

    public long getCreateId() {
        return this.createId;
    }


    /**
     * Get need sync to broker's data
     *
     * @param retValue return data container, ** must not null **
     * @return void
     */
    public void getNeedSyncData(Tuple4<Long,
            Integer, String, Map<String, String>> retValue) {
        brokerSyncData.getBrokerSyncData(retValue);
    }

    /**
     * Book broker report info
     *
     * @param isRegister         request method
     * @param isOnline           if broker is ready
     * @param repConfigId        report configure id
     * @param repCheckSumId      report configure check sum
     * @param isTackData         if tack data in request
     * @param repBrokerConfInfo  tacked broker configure
     * @param repTopicConfs      tacked topic configure
     * @param sBuffer            string process container
     */
    public void bookBrokerReportInfo(boolean isRegister, boolean isOnline,
                                     long repConfigId, int repCheckSumId,
                                     boolean isTackData, String repBrokerConfInfo,
                                     List<String> repTopicConfs,
                                     StringBuilder sBuffer) {
        boolean isSynchronized =
                brokerSyncData.bookBrokerReportInfo(repConfigId,
                        repCheckSumId, isTackData, repBrokerConfInfo, repTopicConfs);
        this.isOnline = isOnline;
        goNextStatus(isRegister, isSynchronized, sBuffer);
    }

    private void goNextStatus(boolean isRegister,
                              boolean isSynchronized,
                              StringBuilder sBuffer) {
        if (isRegister) {
            goRegNextStatus(isSynchronized);
        } else {
            goNonRegNextStatus(isSynchronized);
        }
        execEvent(sBuffer);
    }

    private void execEvent(StringBuilder sBuffer) {
        switch (curStepStatus) {
            case STEP_STATUS_LOAD_DATA: {
                loadNewMetaData(sBuffer);
            }
            break;

            case STEP_STATUS_WAIT_SUBSCRIBE: {
                execSyncDataToSub();
            }
            break;

            case STEP_STATUS_WAIT_PUBLISH: {
                execSyncDataToPub();
            }
            break;

            case STEP_STATUS_UNDEFINED:
            case STEP_STATUS_WAIT_ONLINE:
            case STEP_STATUS_WAIT_SYNC:
            default: {
            }
        }
    }

    private void goRegNextStatus(boolean isSynchronized) {
        resetStatusInfo();
        if (isOnline) {
            if (isSynchronized) {
                curStepStatus = StepStatus.STEP_STATUS_WAIT_SUBSCRIBE;
            } else {
                curStepStatus = StepStatus.STEP_STATUS_WAIT_SYNC;
            }
        } else {
            curStepStatus = StepStatus.STEP_STATUS_WAIT_ONLINE;
        }
    }

    private void goNonRegNextStatus(boolean isSynchronized) {
        switch (curStepStatus) {
            case STEP_STATUS_UNDEFINED: {
                if (!isSynchronized || isDataChanged() || needForceSyncData()) {
                    resetStatusInfo();
                    curStepStatus = StepStatus.STEP_STATUS_LOAD_DATA;
                    nextStepOpTimeInMills =
                            System.currentTimeMillis() + curStepStatus.getDelayDurInMs();
                }
            }
            break;

            case STEP_STATUS_LOAD_DATA: {
                if (isDoneDataLoad) {
                    if (isOnline) {
                        if (isSynchronized) {
                            curStepStatus = StepStatus.STEP_STATUS_WAIT_SUBSCRIBE;
                        } else {
                            curStepStatus = StepStatus.STEP_STATUS_WAIT_SYNC;
                        }
                        nextStepOpTimeInMills =
                                System.currentTimeMillis() + curStepStatus.getDelayDurInMs();
                    }
                }
            }
            break;

            case STEP_STATUS_WAIT_ONLINE: {
                if (isOnline) {
                    if (isSynchronized) {
                        curStepStatus = StepStatus.STEP_STATUS_WAIT_SUBSCRIBE;
                    } else {
                        curStepStatus = StepStatus.STEP_STATUS_WAIT_SYNC;
                    }
                    nextStepOpTimeInMills =
                            System.currentTimeMillis() + curStepStatus.getDelayDurInMs();
                }
            }
            break;

            case STEP_STATUS_WAIT_SYNC: {
                if (isSynchronized) {
                    curStepStatus = StepStatus.STEP_STATUS_WAIT_SUBSCRIBE;
                } else {
                    curStepStatus = StepStatus.STEP_STATUS_WAIT_SYNC;
                }
                nextStepOpTimeInMills =
                        System.currentTimeMillis() + curStepStatus.getDelayDurInMs();
            }
            break;

            case STEP_STATUS_WAIT_SUBSCRIBE: {
                if (isDoneDataSub && System.currentTimeMillis() > nextStepOpTimeInMills) {
                    curStepStatus = StepStatus.STEP_STATUS_WAIT_PUBLISH;
                    nextStepOpTimeInMills =
                            System.currentTimeMillis() + curStepStatus.getDelayDurInMs();
                }
            }
            break;

            case STEP_STATUS_WAIT_PUBLISH:
            default: {
                if (isDoneDataPub && System.currentTimeMillis() > nextStepOpTimeInMills) {
                    finishedDataSync();
                }
            }
        }
    }

    private void loadNewMetaData(StringBuilder sBuffer) {
        if (isDoneDataLoad) {
            return;
        }
        boolean needForceSync = needForceSyncData();
        Tuple3<ManageStatus, String, Map<String, String>> curConfTuple =
                brokerRunManager.getBrokerMetaConfigInfo(brokerInfo.getBrokerId());
        if (TStringUtils.isBlank(curConfTuple.getF1())) {
            statusFsmSamplePrint.printWarn(sBuffer
                    .append("[Broker Sync] found broker(")
                    .append(brokerInfo.getBrokerId())
                    .append(") configure is null").toString());
            sBuffer.delete(0, sBuffer.length());
            return;
        }
        Tuple2<Boolean, Boolean> updResult =
                brokerSyncData.updBrokerSyncData(needForceSync,
                        confChangeNo.get(), curConfTuple.getF0(),
                        curConfTuple.getF1(), curConfTuple.getF2());
        if (updResult == null) {
            return;
        }
        isDoneDataLoad = true;
        this.lastBrokerSyncTime = System.currentTimeMillis();
        if (!updResult.getF0() && !updResult.getF1()) {
            finishedDataSync();
        }
    }

    private void execSyncDataToSub() {
        if (isDoneDataSub) {
            return;
        }
        Tuple2<ManageStatus, Map<String, TopicInfo>> syncData =
                brokerSyncData.getBrokerPublishInfo(brokerInfo);
        brokerRunManager.updBrokerCsmConfInfo(brokerInfo.getBrokerId(),
                syncData.getF0(), syncData.getF1());
        isDoneDataSub = true;
    }

    private void execSyncDataToPub() {
        if (isDoneDataPub) {
            return;
        }
        Tuple2<ManageStatus, Map<String, TopicInfo>> syncData =
                brokerSyncData.getBrokerPublishInfo(brokerInfo);
        brokerRunManager.updBrokerPrdConfInfo(brokerInfo.getBrokerId(),
                syncData.getF0(), syncData.getF1());
        isDoneDataPub = true;
    }

    private void finishedDataSync() {
        this.confLoadedNo.set(brokerSyncData.getDataPushId());
        if (this.confLoadedNo.get() == this.confChangeNo.get()) {
            this.isConfChanged.set(false);
        }
        curStepStatus = StepStatus.STEP_STATUS_UNDEFINED;
        nextStepOpTimeInMills = 0;
        long tmpDuration = System.currentTimeMillis() - curConfLoadTimeInMs;
        if (maxConfLoadedTimeInMs < tmpDuration) {
            maxConfLoadedTimeInMs = tmpDuration;
        }
    }

    private void resetStatusInfo() {
        isDoneDataLoad = false;
        isDoneDataSub = false;
        isDoneDataPub = false;
        nextStepOpTimeInMills = 0;
        curConfLoadTimeInMs = System.currentTimeMillis();
    }

    /**
     * According to last report time and current time to decide if need to report data
     *
     * @return true if need report data otherwise false
     */
    private boolean needForceSyncData() {
        if (System.currentTimeMillis() - this.lastBrokerSyncTime
                > TServerConstants.CFG_REPORT_DEFAULT_SYNC_DURATION) {
            return true;
        }
        return false;
    }

}
