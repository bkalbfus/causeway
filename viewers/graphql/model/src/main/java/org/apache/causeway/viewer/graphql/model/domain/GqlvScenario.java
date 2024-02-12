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
package org.apache.causeway.viewer.graphql.model.domain;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.apache.causeway.viewer.graphql.model.context.Context;

import graphql.schema.GraphQLScalarType;

import lombok.Getter;

/**
 * Exposes a domain service (view model or entity) via the GQL viewer.
 */
public class GqlvScenario implements GqlvScenarioName.Holder, GqlvScenarioGiven.Holder {

    private final Holder holder;
    private final Context context;

    private final GraphQLObjectType.Builder gqlObjectTypeBuilder;
    private final Scenario scenarioPojo;

    private final GqlvScenarioName scenarioName;
    private final GqlvScenarioGiven scenarioGiven;

    @Getter private GraphQLFieldDefinition field;

    private GraphQLObjectType gqlObjectType;

    public GqlvScenario(
            final GqlvScenario.Holder holder,
            final Context context) {
        this.holder = holder;
        this.context = context;

        this.scenarioPojo = context.serviceRegistry.lookupService(Scenario.class).orElseThrow();

        this.gqlObjectTypeBuilder = newObject().name("Scenario");

        this.scenarioName = new GqlvScenarioName(this, context);
        addField(scenarioName.getField());
        this.scenarioGiven = new GqlvScenarioGiven(this, context);
        addField(scenarioGiven.getField());

        this.gqlObjectType = gqlObjectTypeBuilder.build();

        this.field = new GraphQLFieldDefinition.Builder()
                            .name("Scenario")
                            .type(gqlObjectType)
                            .argument(new GraphQLArgument.Builder()
                                                .name("name")
                                                .type(Scalars.GraphQLString)
                            )
                            .build();
    }


    private GraphQLFieldDefinition addField(GraphQLFieldDefinition field) {
        if (field != null) {
            gqlObjectTypeBuilder.field(field);
        }
        return field;
    }

    @Override
    public FieldCoordinates coordinatesFor(GraphQLFieldDefinition fieldDefinition) {
        return coordinates(gqlObjectType, fieldDefinition);
    }

    public void addDataFetchers() {
        context.codeRegistryBuilder.dataFetcher(
                holder.coordinatesFor(getField()),
                (DataFetcher<Object>) environment -> scenarioPojo);

        scenarioName.addDataFetchers();
        scenarioGiven.addDataFetchers();
    }


    @Override
    public String toString() {
        return scenarioPojo.toString();
    }

    public interface Holder
            extends GqlvHolder {
    }
}
