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
package org.apache.causeway.core.metamodel.facets.properties.property.mandatory;

import java.util.Optional;

import org.springframework.lang.Nullable;

import org.apache.causeway.commons.internal.reflection._MethodFacades.MethodFacade;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.objectvalue.mandatory.MandatoryFacet;
import org.apache.causeway.core.metamodel.facets.objectvalue.mandatory.MandatoryFacetAbstract;

import lombok.val;

/**
 * Derived by presence of an {@link Nullable} annotation.
 *
 * <p>
 * This implementation indicates that the {@link FacetHolder} is <i>not</i>
 * mandatory, as per {@link #getSemantics()}.
 */
public class MandatoryFacetInvertedByNullableAnnotationOnProperty
extends MandatoryFacetAbstract {

    public static Optional<MandatoryFacet> create(
            final boolean hasNullable,
            final MethodFacade method,
            final FacetHolder holder) {

        if(!hasNullable) {
            return Optional.empty();
        }

        val returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            return Optional.empty();
        }
        return Optional.of(new MandatoryFacetInvertedByNullableAnnotationOnProperty(holder));
    }

    private MandatoryFacetInvertedByNullableAnnotationOnProperty(final FacetHolder holder) {
        super(holder, Semantics.OPTIONAL);
    }


}
