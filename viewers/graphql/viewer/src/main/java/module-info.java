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
module org.apache.causeway.incubator.viewer.graphql.viewer {
    exports org.apache.causeway.viewer.graphql.viewer;
    exports org.apache.causeway.viewer.graphql.viewer.integration;
    exports org.apache.causeway.viewer.graphql.viewer.controller;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.graphqljava;
    requires jakarta.annotation;
    requires jakarta.inject;
    requires java.net.http;
    requires jakarta.persistence;
    requires static lombok;
    requires org.apache.causeway.applib;
    requires org.apache.causeway.commons;
    requires org.apache.causeway.core.config;
    requires org.apache.causeway.core.metamodel;
    requires org.apache.causeway.incubator.viewer.graphql.model;
    requires org.reactivestreams;
    requires reactor.core;
    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.core;
    requires spring.graphql;
    requires spring.tx;
    requires org.apache.causeway.incubator.viewer.graphql.applib;
    requires spring.web;
    requires org.apache.causeway.core.webapp;
}