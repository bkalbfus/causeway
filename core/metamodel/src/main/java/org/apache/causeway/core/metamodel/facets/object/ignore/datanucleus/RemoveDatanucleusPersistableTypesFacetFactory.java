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
package org.apache.causeway.core.metamodel.facets.object.ignore.datanucleus;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.causeway.commons.internal.collections._Lists;
import org.apache.causeway.commons.internal.factory._InstanceUtil;
import org.apache.causeway.commons.internal.reflection._GenericResolver.ResolvedMethod;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.facetapi.FeatureType;
import org.apache.causeway.core.metamodel.facets.FacetFactoryAbstract;

/**
 * Removes all methods inherited from <tt>org.datanucleus.enhancement.Persistable</tt> (if datanucleus 4.1.x is on the classpath).
 */
public class RemoveDatanucleusPersistableTypesFacetFactory
extends FacetFactoryAbstract {

    private final List<ResolvedMethod> datanucleusPersistableMethodsToIgnore = _Lists.newArrayList();

    @Inject
    public RemoveDatanucleusPersistableTypesFacetFactory(final MetaModelContext mmc) {
        super(mmc, FeatureType.OBJECTS_ONLY);

        final String typeToIgnoreIfOnClasspath = "org.datanucleus.enhancement.Persistable";
        try {
            Class<?> typeToIgnore = _InstanceUtil.loadClass(typeToIgnoreIfOnClasspath);
            addMethodsToBeIgnored(typeToIgnore);
        } catch(Exception ex) {
            // ignore
        }
    }

    private void addMethodsToBeIgnored(final Class<?> typeToIgnore) {
        getClassCache()
            .streamPublicMethods(typeToIgnore)
            .forEach(datanucleusPersistableMethodsToIgnore::add);
    }

    @Override
    public void process(final ProcessClassContext processClassContext) {
        for (final ResolvedMethod mapt : datanucleusPersistableMethodsToIgnore) {
            processClassContext.removeMethod(mapt.name(), null, mapt.paramTypes());
        }
    }

}
