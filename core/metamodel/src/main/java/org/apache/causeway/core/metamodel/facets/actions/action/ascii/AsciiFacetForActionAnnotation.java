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
package org.apache.causeway.core.metamodel.facets.actions.action.ascii;

import java.util.Optional;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.FacetedMethod;
import org.apache.causeway.core.metamodel.facets.all.ascii.AsciiFacet;
import org.apache.causeway.core.metamodel.facets.all.ascii.AsciiFacetAbstract;

public class AsciiFacetForActionAnnotation
extends AsciiFacetAbstract {

    public static Optional<AsciiFacet> create(
            final Optional<Action> collectionIfAny,
            final FacetedMethod facetHolder) {

        return collectionIfAny
                .map(Action::asciiId)
                .filter(_Strings::isNotEmpty)
                .map(asciiId -> new AsciiFacetForActionAnnotation(asciiId, facetHolder));
    }

    private AsciiFacetForActionAnnotation(
            final String value, final FacetHolder holder) {
        super(value, holder);
    }

}
