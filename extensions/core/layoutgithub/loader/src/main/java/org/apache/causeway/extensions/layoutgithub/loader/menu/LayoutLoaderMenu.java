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
package org.apache.causeway.extensions.layoutgithub.loader.menu;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.causeway.applib.CausewayModuleApplib;
import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.NatureOfService;
import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.core.config.CausewayConfiguration;
import org.apache.causeway.extensions.layoutgithub.loader.CausewayModuleExtLayoutGithubLoader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Provides actions to managed the dynamic loading of layouts from a github source code repository.
 * <p>
 *
 * @since 2.x {@index}
 */
@Named(CausewayModuleExtLayoutGithubLoader.NAMESPACE + ".LayoutLoaderMenu")
@DomainService(nature = NatureOfService.VIEW)
@DomainServiceLayout(
        menuBar = DomainServiceLayout.MenuBar.TERTIARY
)
@jakarta.annotation.Priority(PriorityPrecedence.EARLY)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LayoutLoaderMenu {

    public static abstract class ActionDomainEvent<T> extends CausewayModuleApplib.ActionDomainEvent<T> {}

    final CausewayConfiguration causewayConfiguration;

    @Getter
    private boolean enabled;


    @Action(
            commandPublishing = Publishing.DISABLED,
            domainEvent = enableDynamicLayoutLoading.ActionDomainEvent.class,
            executionPublishing = Publishing.DISABLED,
            semantics = SemanticsOf.IDEMPOTENT
    )
    @ActionLayout(
            cssClassFa = "fa-solid fa-toggle-on",
            sequence = "100"
    )
    public class enableDynamicLayoutLoading {

        public class ActionDomainEvent extends LayoutLoaderMenu.ActionDomainEvent<enableDynamicLayoutLoading> {}

        @MemberSupport public void act() {
            LayoutLoaderMenu.this.enabled = true;
        }
        @MemberSupport public boolean hideAct() {
            return !isConfigured();
        }
        @MemberSupport public String disableAct() {
            return LayoutLoaderMenu.this.enabled ? "Already enabled" : null;
        }

    }


    @Action(
            commandPublishing = Publishing.DISABLED,
            domainEvent = enableDynamicLayoutLoading.ActionDomainEvent.class,
            executionPublishing = Publishing.DISABLED,
            semantics = SemanticsOf.IDEMPOTENT
    )
    @ActionLayout(
            cssClassFa = "fa-solid fa-toggle-off",
            sequence = "100"
    )
    public class disableDynamicLayoutLoading {

        public class ActionDomainEvent extends LayoutLoaderMenu.ActionDomainEvent<enableDynamicLayoutLoading> {}

        @MemberSupport public void act() {
            LayoutLoaderMenu.this.enabled = false;
        }
        @MemberSupport public boolean hideAct() {
            return !isConfigured();
        }
        @MemberSupport public String disableAct() {
            return LayoutLoaderMenu.this.enabled ? null : "Already disabled";
        }
    }

    boolean isConfigured() {
        val layoutGithub = causewayConfiguration.getExtensions().getLayoutGithub();
        return layoutGithub.getRepository() != null &&
               layoutGithub.getApiKey() != null;
    }

}
