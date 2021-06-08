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

// package remote defines the remote data which is returned from TubeMQ.
package remote

import (
	"sync"
	"time"

	"github.com/apache/incubator-inlong/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/incubator-inlong/tubemq-client-twins/tubemq-client-go/util"
)

// RmtDataCache represents the data returned from TubeMQ.
type RmtDataCache struct {
	consumerID         string
	groupName          string
	underGroupCtrl     bool
	defFlowCtrlID      int64
	groupFlowCtrlID    int64
	partitionSubInfo   map[string]*metadata.SubscribeInfo
	rebalanceResults   []*metadata.ConsumerEvent
	eventWriteMu       sync.Mutex
	eventReadMu        sync.Mutex
	metaMu             sync.Mutex
	dataBookMu         sync.Mutex
	brokerPartitions   map[*metadata.Node]map[string]bool
	qryPriorityID      int32
	partitions         map[string]*metadata.Partition
	usedPartitions     map[string]int64
	indexPartitions    []string
	partitionTimeouts  map[string]*time.Timer
	topicPartitions    map[string]map[string]bool
	partitionRegBooked map[string]bool
	// EventCh is the channel for consumer to consume
	EventCh chan *metadata.ConsumerEvent
}

// NewRmtDataCache returns a default rmtDataCache.
func NewRmtDataCache() *RmtDataCache {
	r := &RmtDataCache{
		defFlowCtrlID:      util.InvalidValue,
		groupFlowCtrlID:    util.InvalidValue,
		qryPriorityID:      int32(util.InvalidValue),
		partitionSubInfo:   make(map[string]*metadata.SubscribeInfo),
		rebalanceResults:   make([]*metadata.ConsumerEvent, 0, 0),
		brokerPartitions:   make(map[*metadata.Node]map[string]bool),
		partitions:         make(map[string]*metadata.Partition),
		usedPartitions:     make(map[string]int64),
		indexPartitions:    make([]string, 0, 0),
		partitionTimeouts:  make(map[string]*time.Timer),
		topicPartitions:    make(map[string]map[string]bool),
		partitionRegBooked: make(map[string]bool),
		EventCh:            make(chan *metadata.ConsumerEvent, 1),
	}
	return r
}

// GetUnderGroupCtrl returns the underGroupCtrl.
func (r *RmtDataCache) GetUnderGroupCtrl() bool {
	return r.underGroupCtrl
}

// GetDefFlowCtrlID returns the defFlowCtrlID.
func (r *RmtDataCache) GetDefFlowCtrlID() int64 {
	return r.defFlowCtrlID
}

// GetGroupFlowCtrlID returns the groupFlowCtrlID.
func (r *RmtDataCache) GetGroupFlowCtrlID() int64 {
	return r.groupFlowCtrlID
}

// GetGroupName returns the group name.
func (r *RmtDataCache) GetGroupName() string {
	return r.groupName
}

// GetSubscribeInfo returns the partitionSubInfo.
func (r *RmtDataCache) GetSubscribeInfo() []*metadata.SubscribeInfo {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	subInfos := make([]*metadata.SubscribeInfo, 0, len(r.partitionSubInfo))
	for _, sub := range r.partitionSubInfo {
		subInfos = append(subInfos, sub)
	}
	return subInfos
}

// GetQryPriorityID returns the QryPriorityID.
func (r *RmtDataCache) GetQryPriorityID() int32 {
	return r.qryPriorityID
}

// PollEventResult polls the first event result from the rebalanceResults.
func (r *RmtDataCache) PollEventResult() *metadata.ConsumerEvent {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()
	if len(r.rebalanceResults) > 0 {
		event := r.rebalanceResults[0]
		r.rebalanceResults = r.rebalanceResults[1:]
		return event
	}
	return nil
}

// GetPartitionByBroker returns the subscribed partitions of the given broker.
func (r *RmtDataCache) GetPartitionByBroker(broker *metadata.Node) []*metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if partitionMap, ok := r.brokerPartitions[broker]; ok {
		partitions := make([]*metadata.Partition, 0, len(partitionMap))
		for partition := range partitionMap {
			partitions = append(partitions, r.partitions[partition])
		}
		return partitions
	}
	return nil
}

// SetConsumerInfo sets the consumer information including consumerID and groupName.
func (r *RmtDataCache) SetConsumerInfo(consumerID string, group string) {
	r.consumerID = consumerID
	r.groupName = group
}

// UpdateDefFlowCtrlInfo updates the defFlowCtrlInfo.
func (r *RmtDataCache) UpdateDefFlowCtrlInfo(flowCtrlID int64, flowCtrlInfo string) {

}

// UpdateGroupFlowCtrlInfo updates the groupFlowCtrlInfo.
func (r *RmtDataCache) UpdateGroupFlowCtrlInfo(qryPriorityID int32, flowCtrlID int64, flowCtrlInfo string) {

}

// OfferEventAndNotify offers an consumer event and notifies the consumer method and notify the consumer to consume.
func (r *RmtDataCache) OfferEventAndNotify(event *metadata.ConsumerEvent) {
	r.eventReadMu.Lock()
	defer r.eventReadMu.Unlock()
	r.rebalanceResults = append(r.rebalanceResults, event)
	e := r.rebalanceResults[0]
	r.rebalanceResults = r.rebalanceResults[1:]
	r.EventCh <- e
}

// ClearEvent clears all the events.
func (r *RmtDataCache) ClearEvent() {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()
	r.rebalanceResults = r.rebalanceResults[:0]
}

// OfferEventResult offers an consumer event.
func (r *RmtDataCache) OfferEventResult(event *metadata.ConsumerEvent) {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()

	r.rebalanceResults = append(r.rebalanceResults, event)
}

// RemoveAndGetPartition removes the given partitions.
func (r *RmtDataCache) RemoveAndGetPartition(subscribeInfos []*metadata.SubscribeInfo, processingRollback bool, partitions map[*metadata.Node][]*metadata.Partition) {
	if len(subscribeInfos) == 0 {
		return
	}
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	for _, sub := range subscribeInfos {
		partitionKey := sub.GetPartition().GetPartitionKey()
		if partition, ok := r.partitions[partitionKey]; ok {
			if _, ok := r.usedPartitions[partitionKey]; ok {
				if processingRollback {
					partition.SetLastConsumed(false)
				} else {
					partition.SetLastConsumed(true)
				}
			}
			if _, ok := partitions[partition.GetBroker()]; !ok {
				partitions[partition.GetBroker()] = []*metadata.Partition{partition}
			} else {
				partitions[partition.GetBroker()] = append(partitions[partition.GetBroker()], partition)
			}
			r.removeMetaInfo(partitionKey)
		}
		r.resetIdlePartition(partitionKey, false)
	}
}

func (r *RmtDataCache) removeMetaInfo(partitionKey string) {
	if partition, ok := r.partitions[partitionKey]; ok {
		if partitions, ok := r.topicPartitions[partition.GetTopic()]; ok {
			delete(partitions, partitionKey)
			if len(partitions) == 0 {
				delete(r.topicPartitions, partition.GetTopic())
			}
		}
		if partitions, ok := r.brokerPartitions[partition.GetBroker()]; ok {
			delete(partitions, partition.GetPartitionKey())
			if len(partitions) == 0 {
				delete(r.brokerPartitions, partition.GetBroker())
			}
		}
		delete(r.partitions, partitionKey)
		delete(r.partitionSubInfo, partitionKey)
	}
}

func (r *RmtDataCache) resetIdlePartition(partitionKey string, reuse bool) {
	delete(r.usedPartitions, partitionKey)
	if timer, ok := r.partitionTimeouts[partitionKey]; ok {
		if !timer.Stop() {
			<-timer.C
		}
		timer.Stop()
		delete(r.partitionTimeouts, partitionKey)
	}
	r.removeFromIndexPartitions(partitionKey)
	if reuse {
		if _, ok := r.partitions[partitionKey]; ok {
			r.indexPartitions = append(r.indexPartitions, partitionKey)
		}
	}
}

// FilterPartitions returns the unsubscribed partitions.
func (r *RmtDataCache) FilterPartitions(subInfos []*metadata.SubscribeInfo) []*metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	unsubPartitions := make([]*metadata.Partition, 0, len(subInfos))
	if len(r.partitions) == 0 {
		for _, sub := range subInfos {
			unsubPartitions = append(unsubPartitions, sub.GetPartition())
		}
	} else {
		for _, sub := range subInfos {
			if _, ok := r.partitions[sub.GetPartition().GetPartitionKey()]; !ok {
				unsubPartitions = append(unsubPartitions, sub.GetPartition())
			}
		}
	}
	return unsubPartitions
}

// AddNewPartition append a new partition.
func (r *RmtDataCache) AddNewPartition(newPartition *metadata.Partition) {
	sub := &metadata.SubscribeInfo{}
	sub.SetPartition(newPartition)
	sub.SetConsumerID(r.consumerID)
	sub.SetGroup(r.groupName)

	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	partitionKey := newPartition.GetPartitionKey()
	if partition, ok := r.partitions[partitionKey]; !ok {
		r.partitions[partitionKey] = partition
		if partitions, ok := r.topicPartitions[partition.GetPartitionKey()]; !ok {
			newPartitions := make(map[string]bool)
			newPartitions[partitionKey] = true
			r.topicPartitions[partition.GetTopic()] = newPartitions
		} else if _, ok := partitions[partitionKey]; !ok {
			partitions[partitionKey] = true
		}
		if partitions, ok := r.brokerPartitions[partition.GetBroker()]; !ok {
			newPartitions := make(map[string]bool)
			newPartitions[partitionKey] = true
			r.brokerPartitions[partition.GetBroker()] = newPartitions
		} else if _, ok := partitions[partitionKey]; !ok {
			partitions[partitionKey] = true
		}
		r.partitionSubInfo[partitionKey] = sub
	}
	r.resetIdlePartition(partitionKey, true)
}

// HandleExpiredPartitions handles the expired partitions.
func (r *RmtDataCache) HandleExpiredPartitions(wait time.Duration) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	expired := make(map[string]bool, len(r.usedPartitions))
	if len(r.usedPartitions) > 0 {
		curr := time.Now().UnixNano() / int64(time.Millisecond)
		for partition, time := range r.usedPartitions {
			if curr-time > wait.Milliseconds() {
				expired[partition] = true
				if p, ok := r.partitions[partition]; ok {
					p.SetLastConsumed(false)
				}
			}
		}
		if len(expired) > 0 {
			for partition := range expired {
				r.resetIdlePartition(partition, true)
			}
		}
	}
}

// RemovePartition removes the given partition keys.
func (r *RmtDataCache) RemovePartition(partitionKeys []string) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	for _, partitionKey := range partitionKeys {
		r.resetIdlePartition(partitionKey, false)
		r.removeMetaInfo(partitionKey)
	}
}

// IsFirstRegister returns whether the given partition is first registered.
func (r *RmtDataCache) IsFirstRegister(partitionKey string) bool {
	r.dataBookMu.Lock()
	defer r.dataBookMu.Unlock()

	if _, ok := r.partitionRegBooked[partitionKey]; !ok {
		r.partitionRegBooked[partitionKey] = true
	}
	return r.partitionRegBooked[partitionKey]
}

func (r *RmtDataCache) removeFromIndexPartitions(partitionKey string) {
	pos := 0
	for i, p := range r.indexPartitions {
		if p == partitionKey {
			pos = i
			break
		}
	}
	r.indexPartitions = append(r.indexPartitions[:pos], r.indexPartitions[pos+1:]...)
}