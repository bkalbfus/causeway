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
package org.apache.causeway.core.metamodel.facets.properties.update.modify;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.reflection._MethodFacades.MethodFacade;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.ImperativeFacet;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmInvokeUtils;
import org.apache.causeway.core.metamodel.spec.feature.OneToOneAssociation;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

public class PropertySetterFacetViaSetterMethod
extends PropertySetterFacetAbstract
implements ImperativeFacet {

    @Getter(onMethod_ = {@Override}) private final @NonNull Can<MethodFacade> methods;

    public PropertySetterFacetViaSetterMethod(final Method method, final FacetHolder holder) {
        super(holder);
        this.methods = ImperativeFacet.singleRegularMethod(method);
    }

    @Override
    public Intent getIntent() {
        return Intent.MODIFY_PROPERTY;
    }

    @Override
    public ManagedObject setProperty(
            final OneToOneAssociation owningAssociation,
            final ManagedObject targetAdapter,
            final ManagedObject valueAdapter,
            final InteractionInitiatedBy interactionInitiatedBy) {

        val method = methods.getFirstElseFail().asMethodElseFail(); // expected regular
        MmInvokeUtils.invokeWithSingleArg(method, targetAdapter, valueAdapter);
        return targetAdapter;
    }

    @Override
    public void visitAttributes(final BiConsumer<String, Object> visitor) {
        super.visitAttributes(visitor);
        ImperativeFacet.visitAttributes(this, visitor);
    }

}
