/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.causeway.extensions.executionlog.applib.spiimpl;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.services.iactn.Execution;
import org.apache.causeway.applib.services.publishing.spi.ExecutionSubscriber;
import org.apache.causeway.core.config.CausewayConfiguration;
import org.apache.causeway.extensions.executionlog.applib.CausewayModuleExtExecutionLogApplib;
import org.apache.causeway.extensions.executionlog.applib.dom.ExecutionLogEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * @since 2.0 {@index}
 */
@Service
@Named(ExecutionSubscriberForExecutionLog.LOGICAL_TYPE_NAME)
@Priority(PriorityPrecedence.MIDPOINT)
@Qualifier("Default")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Log4j2
public class ExecutionSubscriberForExecutionLog implements ExecutionSubscriber {

    static final String LOGICAL_TYPE_NAME = CausewayModuleExtExecutionLogApplib.NAMESPACE + ".ExecutionSubscriberForExecutionLog";

    final ExecutionLogEntryRepository executionLogEntryRepository;
    final CausewayConfiguration causewayConfiguration;

    @Override
    public boolean isEnabled() {
        return causewayConfiguration.getExtensions().getExecutionLog().getPersist().isEnabled();
    }

    @Override
    public void onExecution(Execution<?, ?> execution) {
        if (!isEnabled()) {
            return;
        }

        executionLogEntryRepository.createEntryAndPersist(execution);
    }

}
