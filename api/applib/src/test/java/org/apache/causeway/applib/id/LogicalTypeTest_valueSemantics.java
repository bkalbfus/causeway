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
package org.apache.causeway.applib.id;

import java.util.List;

import org.apache.causeway.applib.SomeDomainClass;
import org.apache.causeway.core.internaltestsupport.contract.ValueTypeContractTestAbstract;

public class LogicalTypeTest_valueSemantics
extends ValueTypeContractTestAbstract<LogicalType> {

    @Override
    protected List<LogicalType> getObjectsWithSameValue() {
        return List.of(
                LogicalType.fqcn(SomeDomainClass.class),
                LogicalType.eager(SomeDomainClass.class, SomeDomainClass.class.getName()));
    }

    @Override
    protected List<LogicalType> getObjectsWithDifferentValue() {
        return List.of(
                LogicalType.fqcn(Object.class),
                LogicalType.eager(List.class, List.class.getName()));
    }

}
