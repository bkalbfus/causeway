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
package org.apache.causeway.testdomain.viewers.jdo.wkt;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.HomePage;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.user.UserService;
import org.apache.causeway.commons.internal.debug.xray.XrayEnable;
import org.apache.causeway.core.config.presets.CausewayPresets;
import org.apache.causeway.testdomain.conf.Configuration_usingWicket;
import org.apache.causeway.testdomain.jdo.JdoInventoryJaxbVm;
import org.apache.causeway.testdomain.jdo.JdoTestFixtures;
import org.apache.causeway.testdomain.jdo.conf.Configuration_usingJdo;
import org.apache.causeway.testdomain.model.valuetypes.composite.CalendarEventJaxbVm;
import org.apache.causeway.viewer.wicket.viewer.CausewayModuleViewerWicketViewer;

/**
 * Bootstrap the test application.
 */
@SpringBootApplication
@Import({
    Configuration_usingJdo.class,
    Configuration_usingWicket.class,

    // UI (Wicket Viewer)
    CausewayModuleViewerWicketViewer.class,
    //CausewayModuleViewerRestfulObjectsJaxrsResteasy.class,

    XrayEnable.class // for debugging only
})
public class TestAppJdoWkt extends SpringBootServletInitializer {

    /**
     *
     * @param args
     * @implNote this is to support the <em>Spring Boot Maven Plugin</em>, which auto-detects an
     * entry point by searching for classes having a {@code main(...)}
     */
    public static void main(final String[] args) {
        CausewayPresets.prototyping();
        SpringApplication.run(new Class[] { TestAppJdoWkt.class }, args);
    }

    @Named("testdomain.jdo.TestHomePage")
    @DomainObject(
            nature=Nature.VIEW_MODEL)
    @HomePage
    public static class TestHomePage {

        @Inject UserService userService;
        @Inject JdoTestFixtures testFixtures;
        @Inject FactoryService factoryService;

        @ObjectSupport public String title() {
            return "Hello, " + userService.currentUserNameElseNobody();
        }

        @Action @ActionLayout(sequence = "0.1",
                describedAs = "resets the repository to having 3 sample books")
        public JdoInventoryJaxbVm reset() {
            testFixtures.clearRepository();
            testFixtures.add3Books();
            return openSamplePage();
        }

        @Action @ActionLayout(sequence = "0.2")
        public JdoInventoryJaxbVm openSamplePage() {
            return testFixtures.createViewmodelWithCurrentBooks();
        }

        @Action @ActionLayout(sequence = "0.3")
        public CalendarEventJaxbVm openCalendarEventSamplePage() {
            return CalendarEventJaxbVm.setUpViewmodelWith3CalendarEvents(factoryService);
        }

    }

}
