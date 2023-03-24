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
package demoapp.dom.domain.collections.CollectionLayout.paged;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;

import demoapp.dom._infra.asciidocdesc.HasAsciiDocDescription;
import demoapp.dom.domain._entities.DemoEntity;
import lombok.Getter;
import lombok.Setter;

//tag::class[]
@DomainObject(
        nature=Nature.VIEW_MODEL)
@Named("demo.CollectionLayoutPagedVm")
@XmlRootElement(name = "root")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class CollectionLayoutPagedVm implements HasAsciiDocDescription {

    @ObjectSupport public String title() {
        return "CollectionLayout#paged";
    }

//tag::collection[]
    @Collection
    @CollectionLayout(
            paged = 2)
    @XmlTransient
    @Getter @Setter
    private List<DemoEntity> collection = new ArrayList<>();
//end::collection[]

}
//end::class[]
