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
package org.apache.causeway.persistence.jpa.metamodel.facets.prop.column;

import java.util.Optional;

import jakarta.persistence.Column;

import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.objectvalue.digits.MaxTotalDigitsFacet;
import org.apache.causeway.core.metamodel.facets.objectvalue.digits.MaxTotalDigitsFacetAbstract;

public class MaxTotalDigitsFacetFromJpaColumnAnnotation
extends MaxTotalDigitsFacetAbstract {

    public static Optional<MaxTotalDigitsFacet> create(
            final Optional<Column> columnIfAny,
            final FacetHolder holder) {

        return columnIfAny
                .filter(column->column.precision()>0)
                .map(column->
                    new MaxTotalDigitsFacetFromJpaColumnAnnotation(
                            column.precision(), holder));
    }

    private MaxTotalDigitsFacetFromJpaColumnAnnotation(
            final int precision, final FacetHolder holder) {
        super(precision, holder);
    }

}
