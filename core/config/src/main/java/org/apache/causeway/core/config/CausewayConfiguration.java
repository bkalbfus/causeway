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
package org.apache.causeway.core.config;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.activation.DataSource;
import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.validation.annotation.Validated;

import org.apache.causeway.applib.CausewayModuleApplib;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.Introspection.IntrospectionPolicy;
import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.annotation.TableDecorator;
import org.apache.causeway.applib.query.Query;
import org.apache.causeway.applib.services.i18n.Mode;
import org.apache.causeway.applib.services.iactn.Execution;
import org.apache.causeway.applib.services.publishing.spi.CommandSubscriber;
import org.apache.causeway.applib.services.publishing.spi.EntityChangesSubscriber;
import org.apache.causeway.applib.services.publishing.spi.EntityPropertyChangeSubscriber;
import org.apache.causeway.applib.services.publishing.spi.ExecutionSubscriber;
import org.apache.causeway.applib.services.userreg.EmailNotificationService;
import org.apache.causeway.applib.services.userreg.UserRegistrationService;
import org.apache.causeway.applib.services.userui.UserMenu;
import org.apache.causeway.applib.value.semantics.TemporalValueSemantics.TemporalDisplayPattern;
import org.apache.causeway.applib.value.semantics.TemporalValueSemantics.TemporalEditingPattern;
import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.context._Context;
import org.apache.causeway.core.config.metamodel.facets.ActionConfigOptions;
import org.apache.causeway.core.config.metamodel.facets.AssociationLayoutConfigOptions;
import org.apache.causeway.core.config.metamodel.facets.AssociationLayoutConfigOptions.SequencePolicy;
import org.apache.causeway.core.config.metamodel.facets.CollectionLayoutConfigOptions;
import org.apache.causeway.core.config.metamodel.facets.DomainObjectConfigOptions;
import org.apache.causeway.core.config.metamodel.facets.ParameterConfigOptions;
import org.apache.causeway.core.config.metamodel.facets.PropertyConfigOptions;
import org.apache.causeway.core.config.metamodel.services.ApplicationFeaturesInitConfiguration;
import org.apache.causeway.core.config.metamodel.specloader.IntrospectionMode;
import org.apache.causeway.core.config.viewer.web.DialogMode;
import org.apache.causeway.core.config.viewer.web.TextMode;
import org.apache.causeway.schema.cmd.v2.ActionDto;
import org.apache.causeway.schema.cmd.v2.ParamDto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Configuration 'beans' with meta-data (IDE-support).
 *
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">spring.io</a>
 *
 * @since 2.0
 */
@ConfigurationProperties(CausewayConfiguration.ROOT_PREFIX)
@Data
@Validated
public class CausewayConfiguration {

    public static final String ROOT_PREFIX = "causeway";

    private final ConfigurableEnvironment environment;

    /**
     * To ensure that {@link #getBuildProperties()} is full populated, configure the <code>spring-boot-maven-plugin</code>
     * as follows:
     *
     * <pre>
     * &lt;plugin&gt;
     *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
     *     &lt;artifactId&gt;spring-boot-maven-plugin&lt;/artifactId&gt;
     *     &lt;executions&gt;
     *         &lt;execution&gt;
     *             &lt;id&gt;build-info&lt;/id&gt;
     *             &lt;goals&gt;
     *                 &lt;goal&gt;build-info&lt;/goal&gt;
     *             &lt;/goals&gt;
     *             &lt;configuration&gt;
     *                 &lt;additionalProperties&gt;
     *                     &lt;java.version&gt;${java.version}&lt;/java.version&gt;
     *                     &lt;description&gt;${project.description}&lt;/description&gt;
     *                     ...
     *                 &lt;/additionalProperties&gt;
     *             &lt;/configuration&gt;
     *         &lt;/execution&gt;
     *     &lt;/executions&gt;
     *     ...
     * &lt;/plugin&gt;
     * </pre>
     */
    @Getter
    private final Optional<BuildProperties> buildProperties;

    @Builder // for testing
    @Autowired
    public CausewayConfiguration(final ConfigurableEnvironment environment, final Optional<BuildProperties> buildProperties) {
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    /**
     * All known configuration property names.
     *
     * <p>
     *     Or at least, from the {@link org.springframework.core.env.PropertySource} obtained from
     *     {@link ConfigurableEnvironment#getPropertySources()} that are also {@link EnumerablePropertySource}s.
     * </p>
     */
    public Stream<String> streamConfigurationPropertyNames() {
        MutablePropertySources propertySources = environment.getPropertySources();
        return StreamSupport
                .stream(propertySources.spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(EnumerablePropertySource.class::cast)
                .filter(ps->!"systemEnvironment".equalsIgnoreCase(ps.getName())) // exclude system env
                .map(EnumerablePropertySource::getPropertyNames)
                .flatMap(_NullSafe::stream);
    }

    /**
     * The value of a specific configuration property
     *
     * @param configurationPropertyName  - eg as obtained from {@link #streamConfigurationPropertyNames()}.
     */
    public Optional<String> valueOf(final String configurationPropertyName) {
        return Optional.ofNullable(environment.getProperty(configurationPropertyName));
    }

    /**
     * The value of a specific configuration property
     *
     * @param configurationPropertyName  - eg as obtained from {@link #streamConfigurationPropertyNames()}.
     * @param onError - callback in case the value retrieval throws any exception
     */
    public Optional<String> valueOf(final String configurationPropertyName, final @Nullable Consumer<Throwable> onError) {
        try {
            return Optional.ofNullable(environment.getProperty(configurationPropertyName));
        } catch (Throwable t) {
            if(onError!=null) {
                Try.run(()->onError.accept(t));
            }
            return Optional.empty();
        }
        
    }

    private final Security security = new Security();
    @Data
    public static class Security {

        /**
         * If set, allows <i>Actions</i> with SAFE Semantics to be invoked with only VIEWING permissions.
         * <p>
         * default: false
         *
         * @see org.apache.causeway.applib.annotation.Action#semantics()
         * @see SemanticsOf
         */
        private boolean actionsWithSafeSemanticsRequireOnlyViewingPermission = false;

        private final Shiro shiro = new Shiro();
        @Data
        public static class Shiro {
            /**
             * If the Shiro subject is found to be still authenticated, then will be logged out anyway and then
             * re-authenticated.
             *
             * <p>
             * Applies only to the Restful Objects viewer.
             * </p>
             */
            private boolean autoLogoutIfAlreadyAuthenticated = false;

        }

        private final Spring spring = new Spring();
        @Data
        public static class Spring {
            /**
             * The framework on initialization by default disables any {@code CsrfFilter}(s) it finds
             * with <i>Spring Security</i> registered filters.
             * <p>
             * Setting this option to {@literal true} allows {@code CsrfFilter}(s) to be
             * configured. Yet EXPERIMENTAL.
             *
             * @see org.springframework.security.web.csrf.CsrfFilter
             * @see <a href="https://www.baeldung.com/spring-security-registered-filters">baeldung</a>
             */
            private boolean allowCsrfFilters = false;

        }

        private final Keycloak keycloak = new Keycloak();
        @Data
        public static class Keycloak {
            /**
             * The name of the realm for the Apache Causeway application, as configured in
             * Keycloak.
             */
            private String realm;

            /**
             * The base URL for the keycloak server.
             *
             * <p>
             *     For example, if running a keycloak using Docker container, such as:
             *     <pre>
             *         docker run -p 9090:8080 \
             *             -e KEYCLOAK_USER=admin \
             *             -e KEYCLOAK_PASSWORD=admin \
             *             quay.io/keycloak/keycloak:19.0.1
             *     </pre>,
             *
             *     then the URL would be "http://localhost:9090/auth".
             * </p>
             */
            private String baseUrl;

            /**
             * Specifies where users will be redirected after authenticating successfully if they
             * have not visited a secured page prior to authenticating or {@code alwaysUse} is
             * true.
             */
            private String loginSuccessUrl = "/wicket";

            /**
             * Whether to (attempt to) extract realm roles and copy into the <code>DefaultOidcUser</code>.
             *
             * <p>
             *     By default, realm roles are obtained from the token claims using the "User Realm Role" mapping type, into a token claim name "realm_access.roles"
             * </p>
             *
             * <p>
             *     This has been made a configuration option because some versions of Keycloak seemingly do not correctly extract these roles, see for example
             *     <a href="https://keycloak.discourse.group/t/resource-access-claim-missing-from-userinfo-until-i-change-the-name/1238/3">this discussion</a> and
             *     <a href="https://issues.redhat.com/browse/KEYCLOAK-9874">KEYCLOAK-9874</a>.
             * </p>
             */
            private boolean extractRealmRoles = true;

            /**
             * If {@link #isExtractRealmRoles() realm roles are to be extracted}, this allows the resultant role to be optionally prefixed.
             */
            private String realmRolePrefix = null;

            /**
             * Whether to (attempt to) extract client roles and copy into the <code>DefaultOidcUser</code>.
             *
             * <p>
             *     By default, client roles are extracted using the "User Client Role" mapping type, into a token claim name "resource_access.${client_id}.roles"
             * </p>
             *
             * <p>
             *     This has been made a configuration option because some versions of Keycloak seemingly do not correctly extract these roles, see for example
             *     <a href="https://keycloak.discourse.group/t/resource-access-claim-missing-from-userinfo-until-i-change-the-name/1238/3">this discussion</a> and
             *     <a href="https://issues.redhat.com/browse/KEYCLOAK-9874">KEYCLOAK-9874</a>.
             * </p>
             */
            private boolean extractClientRoles = true;
            /**
             * If {@link #isExtractClientRoles()}  client roles are to be extracted}, this allows the resultant role to be optionally prefixed.
             */
            private String clientRolePrefix = null;

            /**
             * Whether to (attempt to) extract any available roles and into the <code>DefaultOidcUser</code>.
             *
             * <p>
             *     This is to support any custom mapping type which maps into a token claim name called simply "roles"
             * </p>
             *
             * <p>
             *     This has been made a configuration option so that the workaround described in
             *     <a href="https://keycloak.discourse.group/t/resource-access-claim-missing-from-userinfo-until-i-change-the-name/1238/3">this discussion</a> and
             *     <a href="https://issues.redhat.com/browse/KEYCLOAK-9874">KEYCLOAK-9874</a> can be implemented.
             * </p>
             */
            private boolean extractRoles = false;
            /**
             * If {@link #isExtractRoles()}  roles are to be extracted}, this allows the resultant role to be optionally prefixed.
             */
            private String rolePrefix = null;
        }
    }

    private final Schema schema = new Schema();
    @Data
    public static class Schema {

        private final Command command = new Command();
        @Data
        public static class Command {

            public enum ParamIdentifierStrategy {
                BY_ID,
                /**
                 * For backward compatibility with v1 behaviour
                 */
                BY_CANONICAL_FRIENDLY_NAME;
            }

            /**
             * Whether the {@link ParamDto#getName()} field - which uniquely identifies a parameter within the
             * {@link org.apache.causeway.schema.cmd.v2.ActionDto action}'s
             * {@link ActionDto#getParameters() list of parameters} - is populated with the parameter's formal Id
             * (eg &quot;firstName&quot;) or instead using the parameter's friendly name (eg &quot;First Name&quot;).
             *
             * <p>
             *     The default is to use the {@link ParamIdentifierStrategy#BY_ID formal Id}, but the name is provided
             *     as an alternative for compatibility with v1.  Note that the name is potentially translated, so this
             *     could also cause issues within integration scenarios.
             * </p>
             */
            private ParamIdentifierStrategy paramIdentifierStrategy = ParamIdentifierStrategy.BY_ID;
        }
    }

    private final Applib applib = new Applib();
    @Data
    public static class Applib {

        private final Annotation annotation = new Annotation();
        @Data
        public static class Annotation {

            private final DomainObject domainObject = new DomainObject();

            public interface ConfigPropsForPropertyOrParameterLayout {
                /**
                 * Defines the default position for the label if not specified through an annotation.
                 *
                 * <p>
                 *     If left as {@link LabelPosition#NOT_SPECIFIED} and not overridden, then the position depends
                 *     upon the viewer implementation.
                 * </p>
                 */
                LabelPosition getLabelPosition();
            }

            @Data
            public static class DomainObject {

                /**
                 * The default for whether <i>domain entities</i> should be audited or not (meaning that any changes are
                 * sent through to {@link EntityChangesSubscriber}s and
                 * sent through to {@link EntityPropertyChangeSubscriber}.
                 *
                 * <p>
                 * This setting can be overridden on a case-by-case basis using {@link org.apache.causeway.applib.annotation.DomainObject#entityChangePublishing()}
                 * </p>
                 *
                 * <p>
                 *     Note: this applies only to domain entities, not view models.
                 * </p>
                 */
                private DomainObjectConfigOptions.EntityChangePublishingPolicy entityChangePublishing = DomainObjectConfigOptions.EntityChangePublishingPolicy.NONE;

                /**
                 * The default for whether the properties of domain objects can be edited, or whether instead they
                 * can be modified only using actions (or programmatically as a side-effect of actions on other objects).
                 *
                 * <p>
                 * This setting can be overridden on a case-by-case basis using {@link DomainObject#getEditing()  DomainObject#getEditing()}
                 * </p>
                 */
                private DomainObjectConfigOptions.EditingObjectsConfiguration editing = DomainObjectConfigOptions.EditingObjectsConfiguration.FALSE;

                private final CreatedLifecycleEvent createdLifecycleEvent = new CreatedLifecycleEvent();
                @Data
                public static class CreatedLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectCreatedEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain object has been created using {@link org.apache.causeway.applib.services.factory.FactoryService}.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#createdLifecycleEvent() @DomainObject(createdLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectCreatedEvent.Noop ObjectCreatedEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectCreatedEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault = true;
                }

                private final LoadedLifecycleEvent loadedLifecycleEvent = new LoadedLifecycleEvent();
                @Data
                public static class LoadedLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectLoadedEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain <i>entity</i> has been loaded from the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#loadedLifecycleEvent() @DomainObject(loadedLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectLoadedEvent.Noop ObjectLoadedEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectLoadedEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

                private final PersistingLifecycleEvent persistingLifecycleEvent = new PersistingLifecycleEvent();
                @Data
                public static class PersistingLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistingEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain <i>entity</i> is about to be persisting (for the first time) to the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#persistingLifecycleEvent() @DomainObject(persistingLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistingEvent.Noop ObjectPersistingEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistingEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

                private final PersistedLifecycleEvent persistedLifecycleEvent = new PersistedLifecycleEvent();
                @Data
                public static class PersistedLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistedEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain <i>entity</i> has been persisted (for the first time) to the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#persistedLifecycleEvent() @DomainObject(persistedLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistedEvent.Noop ObjectPersistedEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectPersistedEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

                private final RemovingLifecycleEvent removingLifecycleEvent = new RemovingLifecycleEvent();
                @Data
                public static class RemovingLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectRemovingEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a persistent domain <i>entity</i> is about to be removed (that is, deleted)
                     * from the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#removingLifecycleEvent() @DomainObject(removingLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectRemovingEvent.Noop ObjectRemovingEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectRemovingEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     *
                     * <p>
                     *     Note: There is no corresponding <code>removed</code> callback, because (for the JDO persistence store at least)
                     *     it is not possible to interact with a domain entity once it has been deleted.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

                private final UpdatedLifecycleEvent updatedLifecycleEvent = new UpdatedLifecycleEvent();
                @Data
                public static class UpdatedLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatedEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a persistent domain <i>entity</i> has been updated in the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#updatedLifecycleEvent() @DomainObject(updatedLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatedEvent.Noop ObjectUpdatedEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatedEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

                private final UpdatingLifecycleEvent updatingLifecycleEvent = new UpdatingLifecycleEvent();
                @Data
                public static class UpdatingLifecycleEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatingEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a persistent domain <i>entity</i> is about to be updated in the persistence store.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObject#updatingLifecycleEvent() @DomainObject(updatingLifecycleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatingEvent.Noop ObjectUpdatingEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.lifecycle.ObjectUpdatingEvent.Default ObjectCreatedEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     Note: this applies only to domain entities, not to view models.
                     * </p>
                     */
                    private boolean postForDefault = true;
                }

            }

            private final DomainObjectLayout domainObjectLayout = new DomainObjectLayout();
            @Data
            public static class DomainObjectLayout {

                /**
                 * Defines the default number of objects that are shown in a &quot;standalone&quot; collection obtained as the
                 * result of invoking an action.
                 *
                 * <p>
                 *     This can be overridden on a case-by-case basis using {@link org.apache.causeway.applib.annotation.DomainObjectLayout#paged()}.
                 * </p>
                 */
                private int paged = 25;

                /**
                 * Defines whether the table representation of a standalone collection of this domain class should be
                 * decorated using a client-side Javascript library, eg for client-side paging and filtering.
                 */
                private Class<? extends TableDecorator> tableDecorator = TableDecorator.Default.class;

                private final CssClassUiEvent cssClassUiEvent = new CssClassUiEvent();
                @Data
                public static class CssClassUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.CssClassUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain object is about to be rendered in the UI - thereby allowing subscribers to
                     * optionally {@link org.apache.causeway.applib.events.ui.CssClassUiEvent#setCssClass(String)} change)
                     * the CSS classes that are used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#cssClassUiEvent()}  @DomainObjectLayout(cssClassEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.CssClassUiEvent.Noop CssClassUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.CssClassUiEvent.Default CssClassUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     The default is <tt>false</tt>, because otherwise the mere presence of <tt>@DomainObjectLayout</tt>
                     *     (perhaps for some attribute other than this one) will cause any imperative <code>cssClass()</code>
                     *     method to be ignored.
                     * </p>
                     */
                    private boolean postForDefault = false;
                }

                private final IconUiEvent iconUiEvent = new IconUiEvent();
                @Data
                public static class IconUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.IconUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain object is about to be rendered in the UI - thereby allowing subscribers to
                     * optionally {@link org.apache.causeway.applib.events.ui.IconUiEvent#setIconName(String)} change)
                     * the icon that is used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#iconUiEvent()}  @DomainObjectLayout(iconEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.IconUiEvent.Noop IconUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.IconUiEvent.Default IconUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     The default is <tt>false</tt>, because otherwise the mere presence of <tt>@DomainObjectLayout</tt>
                     *     (perhaps for some attribute other than this one) will cause any imperative <code>iconName()</code>
                     *     method to be ignored.
                     * </p>
                     */
                    private boolean postForDefault = false;
                }

                private final LayoutUiEvent layoutUiEvent = new LayoutUiEvent();
                @Data
                public static class LayoutUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.LayoutUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain object is about to be rendered in the UI - thereby allowing subscribers to
                     * optionally {@link org.apache.causeway.applib.events.ui.LayoutUiEvent#setLayout(String)} change)
                     * the layout that is used.
                     *
                     * <p>
                     *     If a different layout value has been set, then a layout in the form <code>Xxx.layout-zzz.xml</code>
                     *     use used (where <code>zzz</code> is the name of the layout).
                     * </p>
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#layoutUiEvent()}  @DomainObjectLayout(layoutEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.LayoutUiEvent.Noop LayoutUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.LayoutUiEvent.Default LayoutUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     The default is <tt>false</tt>, because otherwise the mere presence of <tt>@DomainObjectLayout</tt>
                     *     (perhaps for some attribute other than this one) will cause any imperative <code>layout()</code>
                     *     method to be ignored.
                     * </p>
                     */
                    private boolean postForDefault = false;
                }

                private final TitleUiEvent titleUiEvent = new TitleUiEvent();
                @Data
                public static class TitleUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.TitleUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a domain object is about to be rendered in the UI - thereby allowing subscribers to
                     * optionally {@link org.apache.causeway.applib.events.ui.TitleUiEvent#setTitle(String)} change)
                     * the title that is used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#titleUiEvent()}  @DomainObjectLayout(titleEvent=...)} for the
                     *     domain object in question.
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.TitleUiEvent.Noop TitleUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.TitleUiEvent.Default TitleUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     *
                     * <p>
                     *     The default is <tt>false</tt>, because otherwise the mere presence of <tt>@DomainObjectLayout</tt>
                     *     (perhaps for some attribute other than this one) will cause any imperative <code>title()</code>
                     *     method to be ignored.
                     * </p>
                     */
                    private boolean postForDefault = false;
                }
            }

            private final Action action = new Action();
            @Data
            public static class Action {

                /**
                 * The default for whether action invocations should be reified
                 * as a {@link org.apache.causeway.applib.services.command.Command},
                 * to be sent to any registered
                 * {@link CommandSubscriber}s,
                 * typically for auditing purposes.
                 *
                 * <p>
                 *  This setting can be overridden on a case-by-case basis using
                 *  {@link org.apache.causeway.applib.annotation.Action#commandPublishing()}.
                 * </p>
                 */
                private ActionConfigOptions.PublishingPolicy commandPublishing = ActionConfigOptions.PublishingPolicy.NONE;

                /**
                 * The default for whether action invocations should be sent through to the
                 * {@link ExecutionSubscriber} for publishing.
                 *
                 * <p>
                 *     The service's {@link ExecutionSubscriber#onExecution(Execution) onExecution}
                 *     method is called only once per transaction, with
                 *     {@link Execution} collecting details of
                 *     the identity of the target object, the action invoked, the action arguments and the returned
                 *     object (if any).
                 * </p>
                 *
                 * <p>
                 *  This setting can be overridden on a case-by-case basis using {@link org.apache.causeway.applib.annotation.Action#executionPublishing()  Action#executionPublishing()}.
                 * </p>
                 */
                private ActionConfigOptions.PublishingPolicy executionPublishing = ActionConfigOptions.PublishingPolicy.NONE;

                private final DomainEvent domainEvent = new DomainEvent();
                @Data
                public static class DomainEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.domain.ActionDomainEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever an action is being interacted with.
                     *
                     * <p>
                     *     Up to five different events can be fired during an interaction, with the event's
                     *     {@link org.apache.causeway.applib.events.domain.ActionDomainEvent#getEventPhase() phase}
                     *     determining which (hide, disable, validate, executing and executed).  Subscribers can
                     *     influence the behaviour at each of these phases.
                     * </p>
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is actually sent depends
                     *     on the value of the {@link org.apache.causeway.applib.annotation.Action#domainEvent()} for the
                     *     action in question
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.ActionDomainEvent.Noop ActionDomainEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.ActionDomainEvent.Default ActionDomainEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault = true;
                }

            }

            private final ActionLayout actionLayout = new ActionLayout();
            @Data
            public static class ActionLayout {

                private final CssClass cssClass = new CssClass();
                @Data
                public static class CssClass {
                    /**
                     * Provides a mapping of patterns to CSS classes, where the pattern is used to match against the
                     * name of the action method in order to determine a CSS class to use, for example on the action's
                     * button if rendered by the Wicket viewer.
                     *
                     * <p>
                     *     Providing a default set of patterns encourages a common set of verbs to be used.
                     * </p>
                     *
                     * <p>
                     *     The CSS class for individual actions can be overridden using
                     *     {@link org.apache.causeway.applib.annotation.ActionLayout#cssClass()}.
                     * </p>
                     */
                    private String[] patterns = {
                            "add.*:btn-info",
                            "remove.*:btn-warning",

                            "start.*:btn-info",
                            "play.*:btn-info",
                            "stop.*:btn-warning",

                            "reset.*:btn-warning",

                            "new.*:btn-info",
                            "create.*:btn-info",
                            "delete.*:btn-danger",

                            "verify.*:btn-success",
                            "decline.*:btn-danger",

                            "save.*:btn-success",

                            "approve.*:btn-success",
                            "reject.*:btn-danger",

                    };

                    @Getter(lazy = true)
                    private final Map<Pattern, String> patternsAsMap = asMap(getPatterns());

                }

                private final CssClassFa cssClassFa = new CssClassFa();
                @Data
                public static class CssClassFa {
                    /**
                     * Provides a mapping of patterns to font-awesome CSS classes, where the pattern is used to match
                     * against the name of the action method in order to determine a CSS class to use, for example on
                     * the action's menu icon if rendered by the Wicket viewer.
                     *
                     * <p>
                     *     Providing a default set of patterns encourages a common set of verbs to be used.
                     * </p>
                     *
                     * <p>
                     *     The font awesome class for individual actions can be overridden using
                     *     {@link org.apache.causeway.applib.annotation.ActionLayout#cssClassFa()}.
                     * </p>
                     */
                    private String[] patterns = {

                            "all.*:fa-solid fa-list",
                            "list.*:fa-solid fa-list",

                            "find.*:fa-search",
                            "lookup.*:fa-search",
                            "search.*:fa-search",

                            "send.*:fa-regular fa-paper-plane",

                            "open.*:fa-solid fa-arrow-up-right-from-square",
                            "close.*:fa-solid fa-regular fa-rectangle-xmark",

                            "recent.*:fa-solid fa-clock-rotate-left",

                            "lock.*:fa-solid fa-lock",
                            "unlock.*:fa-solid fa-unlock",

                            "permit.*:fa-solid fa-unlock",
                            "review.*:fa-solid fa-eye",

                            "add.*:fa-regular fa-square-plus",
                            "plus.*:fa-regular fa-square-plus",
                            "remove.*:fa-regular fa-square-minus",
                            "minus.*:fa-regular fa-square-minus",

                            "sign.*:fa-solid fa-signature",

                            "clear.*:fa-solid fa-broom",

                            "create.*:fa-regular fa-square-plus",
                            "new.*:fa-regular fa-square-plus",
                            "delete.*:fa-solid fa-trash",

                            "change.*:fa-regular fa-pen-to-square",
                            "edit.*:fa-regular fa-pen-to-square",
                            "maintain.*:fa-regular fa-pen-to-square",
                            "update.*:fa-regular fa-pen-to-square",

                            "cut.*:fa-solid fa-scissors",
                            "move.*:fa-solid fa-angles-right",
                            "copy.*:fa-regular fa-copy",
                            "duplicate.*:fa-solid fa-clone",
                            "clone.*:fa-solid fa-clone",
                            "categorise.*:fa-regular fa-folder-open",

                            "download.*:fa-solid fa-download",
                            "upload.*:fa-solid fa-upload",

                            "execute.*:fa-solid fa-bolt",
                            "run.*:fa-solid fa-bolt",
                            "trigger.*:fa-solid fa-bolt",

                            "link.*:fa-solid fa-link",
                            "unlink.*:fa-solid fa-link-slash",

                            "start.*:fa-solid fa-play",
                            "play.*:fa-solid fa-play",
                            "resume.*:fa-solid fa-play",
                            "pause.*:fa-solid fa-pause",
                            "suspend.*:fa-solid fa-pause",
                            "stop.*:fa-solid fa-stop",
                            "terminate.*:fa-solid fa-stop",

                            "previous.*:fa-backward-step",
                            "next.*:fa-forward-step",

                            "approve.*:fa-regular fa-thumbs-up",
                            "reject.*:fa-regular fa-thumbs-down",

                            "verify.*:fa-solid fa-check",
                            "decline.*:fa-solid fa-xmark",
                            "cancel.*:fa-solid fa-xmark",

                            "discard.*:fa-regular fa-trash-can",

                            "assign.*:fa-regular fa-hand-point-right",

                            "calculate.*:fa-calculator",

                            "import.*:fa-solid fa-file-import",
                            "export.*:fa-solid fa-file-export",

                            "first.*:fa-regular fa-star",

                            "install.*:fa-solid fa-wrench",

                            "setup.*:fa-solid fa-gear",
                            "configure.*:fa-solid fa-gear",

                            "refresh.*:fa-sync",
                            "renew.*:fa-rotate-right",
                            "reset.*:fa-rotate-left",

                            "save.*:fa-regular fa-floppy-disk",

                            "switch.*:fa-exchange",
                            "random.*:fa-shuffle",

                            "view.*:fa-regular fa-eye",

                            "wizard.*:fa-solid fa-wand-magic-sparkles"

                    };

                    @Getter(lazy = true)
                    private final Map<Pattern, String> patternsAsMap = asMap(getPatterns());

                }
            }

            private final Property property = new Property();
            @Data
            public static class Property {

                /**
                 * The default for whether property edits should be reified
                 * as a {@link org.apache.causeway.applib.services.command.Command},
                 * to be sent to any registered
                 * {@link CommandSubscriber}s,
                 * either for auditing or for replayed against a secondary
                 * system, eg for regression testing.
                 *
                 * <p>
                 *  This setting can be overridden on a case-by-case basis using
                 *  {@link org.apache.causeway.applib.annotation.Property#commandPublishing()}.
                 * </p>
                 */
                private PropertyConfigOptions.PublishingPolicy commandPublishing = PropertyConfigOptions.PublishingPolicy.NONE;

                /**
                 * The default for whether property edits should be sent through to the
                 * {@link ExecutionSubscriber} for publishing.
                 *
                 * <p>
                 * The service's {@link ExecutionSubscriber#onExecution(Execution)}  publish}
                 * method is called only once per transaction, with
                 * {@link Execution} collecting details of
                 * the identity of the target object, the property edited, and the new value of the property.
                 * </p>
                 *
                 * <p>
                 * This setting can be overridden on a case-by-case basis using
                 * {@link org.apache.causeway.applib.annotation.Property#executionPublishing()}.
                 * </p>
                 */
                private PropertyConfigOptions.PublishingPolicy executionPublishing = PropertyConfigOptions.PublishingPolicy.NONE;

                private final DomainEvent domainEvent = new DomainEvent();
                @Data
                public static class DomainEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.domain.PropertyDomainEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever an property is being interacted with.
                     *
                     * <p>
                     *     Up to five different events can be fired during an interaction, with the event's
                     *     {@link org.apache.causeway.applib.events.domain.PropertyDomainEvent#getEventPhase() phase}
                     *     determining which (hide, disable, validate, executing and executed).  Subscribers can
                     *     influence the behaviour at each of these phases.
                     * </p>
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is actually sent depends
                     *     on the value of the {@link org.apache.causeway.applib.annotation.Property#domainEvent()} for the
                     *     property in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.PropertyDomainEvent.Noop propertyDomainEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.PropertyDomainEvent.Default propertyDomainEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault = true;
                }

            }

            private final PropertyLayout propertyLayout = new PropertyLayout();
            @Data
            public static class PropertyLayout implements Applib.Annotation.ConfigPropsForPropertyOrParameterLayout {
                /**
                 * Defines the default position for the label for a domain object property.
                 *
                 * <p>
                 *     Can be overridden on a case-by-case basis using
                 *     {@link org.apache.causeway.applib.annotation.ParameterLayout#labelPosition()}.
                 * </p>
                 *
                 * <p>
                 *     If left as {@link LabelPosition#NOT_SPECIFIED} and not overridden, then the position depends
                 *     upon the viewer implementation.
                 * </p>
                 */
                private LabelPosition labelPosition = LabelPosition.NOT_SPECIFIED;

                /**
                 * How {@link org.apache.causeway.applib.annotation.PropertyLayout#sequence()}
                 * should be handled when calculating the slot-in order for unreferenced <i>Properties</i>.
                 * {@code AS_PER_SEQUENCE} will use Dewey order based on available 'sequence' attributes,
                 * whereas {@code ALPHABETICALLY} will use alphabetical order based on member names.
                 * <p>
                 * default: {@code AS_PER_SEQUENCE}
                 */
                private AssociationLayoutConfigOptions.SequencePolicy sequencePolicyIfUnreferenced = SequencePolicy.AS_PER_SEQUENCE;
            }

            private final Collection collection = new Collection();
            @Data
            public static class Collection {

                private final DomainEvent domainEvent = new DomainEvent();
                @Data
                public static class DomainEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.domain.CollectionDomainEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a collection is being interacted with.
                     *
                     * <p>
                     *     Up to two different events can be fired during an interaction, with the event's
                     *     {@link org.apache.causeway.applib.events.domain.CollectionDomainEvent#getEventPhase() phase}
                     *     determining which (hide, disable)Subscribers can influence the behaviour at each of these
                     *     phases.
                     * </p>
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is actually sent depends
                     *     on the value of the {@link org.apache.causeway.applib.annotation.Collection#domainEvent()} for the
                     *     collection action in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.CollectionDomainEvent.Noop CollectionDomainEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.domain.CollectionDomainEvent.Default CollectionDomainEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault = true;
                }
            }

            private final CollectionLayout collectionLayout = new CollectionLayout();
            @Data
            public static class CollectionLayout {

                /**
                 * Defines the initial view to display collections when rendered.
                 *
                 * <p>
                 *     The value of this can be overridden on a case-by-case basis using
                 *     {@link org.apache.causeway.applib.annotation.CollectionLayout#defaultView()}.
                 *     Note that this default configuration property is an enum and so defines only a fixed number of
                 *     values, whereas the annotation returns a string; this is to allow for flexibility that
                 *     individual viewers might support their own additional types.  For example, the Wicket viewer
                 *     supports <codefullcalendar</code> which can render objects that have a date on top of a calendar
                 *     view.
                 * </p>
                 */
                private CollectionLayoutConfigOptions.DefaultView defaultView = CollectionLayoutConfigOptions.DefaultView.TABLE;

                /**
                 * Defines the default number of objects that are shown in a &quot;parented&quot; collection of a
                 * domain object,
                 * result of invoking an action.
                 *
                 * <p>
                 *     This can be overridden on a case-by-case basis using
                 *     {@link org.apache.causeway.applib.annotation.CollectionLayout#paged()}.
                 * </p>
                 */
                private int paged = 12;

                /**
                 * How {@link org.apache.causeway.applib.annotation.CollectionLayout#sequence()}
                 * should be handled when calculating the slot-in order for unreferenced <i>Collections</i>.
                 * {@code AS_PER_SEQUENCE} will use Dewey order based on available 'sequence' attributes,
                 * whereas {@code ALPHABETICALLY} will use alphabetical order based on member names.
                 * <p>
                 * default: {@code AS_PER_SEQUENCE}
                 */
                private AssociationLayoutConfigOptions.SequencePolicy sequencePolicyIfUnreferenced =
                        SequencePolicy.AS_PER_SEQUENCE;

                /**
                 * Defines whether the table representation of a collection should be decorated using a client-side
                 * Javascript library, eg for client-side paging and filtering.
                 */
                private Class<? extends TableDecorator> tableDecorator = TableDecorator.Default.class;

            }

            private final ViewModel viewModel = new ViewModel();
            @Data
            public static class ViewModel {
                private final Validation validation = new Validation();
                @Data
                public static class Validation {
                    private final SemanticChecking semanticChecking = new SemanticChecking();
                    @Data
                    public static class SemanticChecking {
                        /**
                         * Whether to check for inconsistencies between the usage of
                         * {@link org.apache.causeway.applib.annotation.DomainObject} and
                         * {@link org.apache.causeway.applib.annotation.DomainObjectLayout}.
                         */
                        private boolean enable = false;
                    }
                }
            }

            private final ViewModelLayout viewModelLayout = new ViewModelLayout();
            @Data
            public static class ViewModelLayout {

                private final CssClassUiEvent cssClassUiEvent = new CssClassUiEvent();
                @Data
                public static class CssClassUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.CssClassUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a view model (annotated with
                     * {@link org.apache.causeway.applib.annotation.DomainObject#nature() @DomainObject#nature} of
                     * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL}) is about to be rendered in the
                     * UI - thereby allowing subscribers to optionally
                     * {@link org.apache.causeway.applib.events.ui.CssClassUiEvent#setCssClass(String)} change) the CSS
                     * classes that are used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#cssClassUiEvent() @DomainObjectLayout(cssClassEvent=...)}
                     *     for the domain object in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.CssClassUiEvent.Noop CssClassUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.CssClassUiEvent.Default CssClassUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault =true;
                }

                private final IconUiEvent iconUiEvent = new IconUiEvent();
                @Data
                public static class IconUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.IconUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a view model (annotated with
                     * {@link org.apache.causeway.applib.annotation.DomainObject#nature() @DomainObject#nature} of
                     * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL})  is about to be rendered in the
                     * UI - thereby allowing subscribers to optionally
                     * {@link org.apache.causeway.applib.events.ui.IconUiEvent#setIconName(String)} change) the icon that
                     * is used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#iconUiEvent() @ViewModelLayout(iconEvent=...)}
                     *     for the domain object in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.IconUiEvent.Noop IconUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.IconUiEvent.Default IconUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault =true;
                }

                private final LayoutUiEvent layoutUiEvent = new LayoutUiEvent();
                @Data
                public static class LayoutUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.LayoutUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a view model (annotated with
                     * {@link org.apache.causeway.applib.annotation.DomainObject#nature() @DomainObject#nature} of
                     * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL})  is about to be rendered in the
                     * UI - thereby allowing subscribers to optionally
                     * {@link org.apache.causeway.applib.events.ui.LayoutUiEvent#setLayout(String)} change) the layout that is used.
                     *
                     * <p>
                     *     If a different layout value has been set, then a layout in the form <code>Xxx.layout-zzz.xml</code>
                     *     use used (where <code>zzz</code> is the name of the layout).
                     * </p>
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#layoutUiEvent() @DomainObjectLayout(layoutEvent=...)}
                     *     for the domain object in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.LayoutUiEvent.Noop LayoutUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.LayoutUiEvent.Default LayoutUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault =true;
                }

                private final TitleUiEvent titleUiEvent = new TitleUiEvent();
                @Data
                public static class TitleUiEvent {
                    /**
                     * Influences whether an {@link org.apache.causeway.applib.events.ui.TitleUiEvent} should
                     * be published (on the internal {@link org.apache.causeway.applib.services.eventbus.EventBusService})
                     * whenever a view model (annotated with
                     * {@link org.apache.causeway.applib.annotation.DomainObject#nature() @DomainObject#nature} of
                     * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL})  is about to be rendered in the
                     * UI - thereby allowing subscribers to
                     * optionally {@link org.apache.causeway.applib.events.ui.TitleUiEvent#setTitle(String)} change)
                     * the title that is used.
                     *
                     * <p>
                     *     The algorithm for determining whether (and what type of) an event is sent depends on the value of the
                     *     {@link org.apache.causeway.applib.annotation.DomainObjectLayout#titleUiEvent() @DomainObjectLayout(titleEvent=...)} for the
                     *     domain object in question:
                     * </p>
                     *
                     * <ul>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.TitleUiEvent.Noop TitleUiEvent.Noop},
                     *         then <i>no</i> event is sent.
                     *     </li>
                     *     <li>
                     *         If set to some subtype of
                     *         {@link org.apache.causeway.applib.events.ui.TitleUiEvent.Default TitleUiEvent.Default},
                     *         then an event is sent <i>if and only if</i> this configuration setting is set.
                     *     </li>
                     *     <li>
                     *         If set to any other subtype, then an event <i>is</i> sent.
                     *     </li>
                     * </ul>
                     */
                    private boolean postForDefault =true;
                }
            }

            private final Parameter parameter = new Parameter();
            @Data
            public static class Parameter {

                /**
                 * Whether parameters should be reset to their default if an earlier parameter changes its
                 * value, or whether instead a parameter value, once changed by the end-user, should never be
                 * overwritten even if the end-user changes an earlier parameter value.
                 *
                 * <p>
                 *     This setting can be overridden on a case-by-case basis using
                 *     {@link org.apache.causeway.applib.annotation.Parameter#precedingParamsPolicy() Parameter#precedingParametersPolicy()}.
                 * </p>
                 */
                private ParameterConfigOptions.PrecedingParametersPolicy precedingParametersPolicy = ParameterConfigOptions.PrecedingParametersPolicy.RESET;

            }

            private final ParameterLayout parameterLayout = new ParameterLayout();
            @Data
            public static class ParameterLayout implements Applib.Annotation.ConfigPropsForPropertyOrParameterLayout {
                /**
                 * Defines the default position for the label for an action parameter.
                 *
                 * <p>
                 *     Can be overridden on a case-by-case basis using
                 *     {@link org.apache.causeway.applib.annotation.ParameterLayout#labelPosition()}.
                 * </p>
                 *
                 * <p>
                 *     If left as {@link LabelPosition#NOT_SPECIFIED} and not overridden, then the position depends
                 *     upon the viewer implementation.
                 * </p>
                 */
                private LabelPosition labelPosition = LabelPosition.NOT_SPECIFIED;
            }

        }

        private final Service service = new Service();
        @Data
        public static class Service {

            private final MetricsService metricsService = new MetricsService();
            @Data
            public static class MetricsService {
                public enum Level {
                    COUNTERS_ONLY,
                    COUNTERS_AND_DETAIL;

                    public boolean isCountersOnly() { return this == COUNTERS_ONLY; }
                    public boolean isCountersAndDetail() { return this == COUNTERS_AND_DETAIL; }
                }

                /**
                 * What level of detail the MetricsService should capture.
                 */
                private Level level = Level.COUNTERS_ONLY;
            }
        }
    }

    private final Core core = new Core();
    @Data
    public static class Core {

        private final Config config = new Config();
        @Data
        public static class Config {

            public static enum ConfigurationPropertyVisibilityPolicy {
                NEVER_SHOW,
                SHOW_ONLY_IN_PROTOTYPE,
                ALWAYS_SHOW
            }

            /**
             * Configuration values might contain sensitive data, hence per default,
             * configuration properties are only visible with the configuration-page
             * when <i>prototyping</i>.
             *
             * <p>
             * Alternatively this policy can be set to either <b>always</b> show or <b>never</b> show.
             * </p>
             *
             * @see ConfigurationPropertyVisibilityPolicy
             */
            private ConfigurationPropertyVisibilityPolicy configurationPropertyVisibilityPolicy
                    = ConfigurationPropertyVisibilityPolicy.SHOW_ONLY_IN_PROTOTYPE;

        }

        private final MetaModel metaModel = new MetaModel();
        @Data
        public static class MetaModel {

            /**
             * Whether domain objects to which the current user does not have visibility access should be rendered
             * within collections or drop-down choices/autocompletes.
             *
             * <p>
             *     One reason this filtering may be necessary is for multi-tenanted applications, whereby an end-user
             *     should only be able to "see" what data that they own.  For efficiency, the application should
             *     only query for objects that the end-user owns.  This configuration property acts as a safety net to
             *     prevent the end-user from viewing domain objects <i>even if</i> those domain objects were rehydrated
             *     from the persistence store.
             * </p>
             */
            private boolean filterVisibility = true;

            private final ProgrammingModel programmingModel = new ProgrammingModel();
            @Data
            public static class ProgrammingModel {

                /**
                 * If set, then any aspects of the programming model (as implemented by <code>FacetFactory</code>s that
                 * have been indicated as deprecated will simply be ignored/excluded from the metamodel.
                 */
                private boolean ignoreDeprecated = false;
            }

            private final Introspector introspector = new Introspector();
            @Data
            public static class Introspector {

                /**
                 * Policy as to how introspection should process
                 * class members and supporting methods.
                 * <p>
                 * Default is to only introspect public class members, while annotating these is optional.
                 */
                private IntrospectionPolicy policy;
                public IntrospectionPolicy getPolicy() {
                    return Optional.ofNullable(policy)
                            .orElse(IntrospectionPolicy.ANNOTATION_OPTIONAL);
                }

                /**
                 * Whether to perform metamodel introspection in parallel, intended to speed up bootstrapping.
                 *
                 * <p>
                 *     For now this is <i>experimental</i>.
                 *     We recommend this is left as disabled (the default).
                 * </p>
                 */
                private boolean parallelize = false; //TODO[CAUSEWAY-2382] concurrent spec-loading is experimental

                /**
                 * Whether all known types should be fully introspected as part of the bootstrapping, or should only be
                 * partially introspected initially.
                 *
                 * <p>
                 * Leaving this as lazy means that there's a chance that metamodel validation errors will not be
                 * discovered during bootstrap.  That said, metamodel validation is still run incrementally for any
                 * classes introspected lazily after initial bootstrapping (unless {@link #isValidateIncrementally()} is
                 * disabled.
                 * </p>
                 */
                private IntrospectionMode mode = IntrospectionMode.LAZY_UNLESS_PRODUCTION;

                /**
                 * If true, then no new specifications will be allowed to be loaded once introspection has been complete.
                 *
                 * <p>
                 * Only applies if the introspector is configured to perform full introspection up-front (either because of
                 * {@link IntrospectionMode#FULL} or {@link IntrospectionMode#LAZY_UNLESS_PRODUCTION} when in production);
                 * otherwise is ignored.
                 * </p>
                 */
                private boolean lockAfterFullIntrospection = true;

                /**
                 * If true, then metamodel validation is performed after any new specification has been loaded (after the
                 * initial bootstrapping).
                 *
                 * <p>
                 * This does <i>not</i> apply if the introspector is configured to perform full introspection up-front
                 * AND when the metamodel is {@link Core.MetaModel.Introspector#isLockAfterFullIntrospection() locked} after initial bootstrapping
                 * (because in that case the lock check will simply prevent any new specs from being loaded).
                 * But it will apply otherwise.
                 * </p>
                 *
                 * <p>In particular, this setting <i>can</i> still apply even if the {@link Core.MetaModel.Introspector#getMode() introspection mode}
                 * is set to {@link IntrospectionMode#FULL full}, because that in itself does not preclude some code
                 * from attempting to load some previously unknown type.  For example, a fixture script could attempt to
                 * invoke an action on some new type using the
                 * {@link org.apache.causeway.applib.services.wrapper.WrapperFactory} - this will cause introspection of that
                 * new type to be performed.
                 * </p>
                 */
                private boolean validateIncrementally = true;

            }

            private final Validator validator = new Validator();
            @Data
            public static class Validator {

                /**
                 * Whether to perform metamodel validation in parallel.
                 */
                private boolean parallelize = true;

                /**
                 * This setting is used to determine whether the use of such deprecated features is
                 * allowed.
                 *
                 * <p>
                 *     If not allowed, then metamodel validation errors will be flagged.
                 * </p>
                 *
                 * <p>
                 *     Note that this settings has no effect if the programming model has been configured to
                 *     {@link ProgrammingModel#isIgnoreDeprecated() ignore deprecated} features (because in this case
                 *     the programming model facets simply won't be included in the introspection process.
                 * </p>
                 */
                private boolean allowDeprecated = true;

                /**
                 * Whether to validate that any actions that accept action parameters have either a corresponding
                 * choices or auto-complete for that action parameter, or are associated with a collection of the
                 * appropriate type.
                 */
                private boolean actionCollectionParameterChoices = true;

                /**
                 * Whether to ensure that the logical-type-name of all objects must be specified explicitly, using either
                 * {@link Named}.
                 *
                 * <p>
                 *     It is <i>highly advisable</i> to leave this set as enabled (the default). These logical-type-names
                 *     should also (of course) be unique - among non-abstract types.
                 * </p>
                 */
                private boolean explicitLogicalTypeNames = false;

                /**
                 * Allows logical type name in {@link Named} also be included in the list of {@link DomainObject#aliased()}
                 * or {@link DomainService#aliased()}.
                 *
                 * <p>
                 *     It is <i>highly advisable</i> to leave this disabled. This option is meant as a practical way to
                 *     enable to transition from old names to new logical type names. Especially when you have a large
                 *     number of files that have to migrated and you want to do the migration in incremental steps.
                 * </p>
                 */
                private boolean allowLogicalTypeNameAsAlias = false;

                private final JaxbViewModel jaxbViewModel = new JaxbViewModel();
                @Data
                public static class JaxbViewModel {
                    /**
                     * If set, then ensures that all JAXB-style view models are concrete classes, not abstract.
                     */
                    private boolean notAbstract = true;
                    /**
                     * If set, then ensures that all JAXB-style view models are either top-level classes or nested
                     * static classes (in other words, checks that they are not anonymous, local nor nested
                     * non-static classes).
                     */
                    private boolean notInnerClass = true;
                    /**
                     * If set, then ensures that all JAXB-style view models have a no-arg constructor.
                     */
                    private boolean noArgConstructor = false;
                    /**
                     * If set, then ensures that for all properties of JAXB-style view models where the property's type
                     * is an entity, then that entity's type has been correctly annotated with
                     * @{@link jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter} (so that the property's value can
                     * be converted into a serializable form).
                     */
                    private boolean referenceTypeAdapter = true;
                    /**
                     * If set, then ensures that for all properties of JAXB-style view models where the property's type
                     * is a date or time, then that property has been correctly annotated with
                     * @{@link jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter} (so that the property's value can
                     * be converted into a serializable form).
                     */
                    private boolean dateTimeTypeAdapter = true;
                }

                private final Jdoql jdoql = new Jdoql();
                @Data
                public static class Jdoql {
                    /**
                     * If set, then ensures that the 'FROM' clause within any JDOQL <code>@Query</code>s annotations
                     * relates to a known entity type, and moreover that that type is compatible with the type on
                     * which the annotation appears: meaning its either a supertype of or the same type as the
                     * annotated type.
                     */
                    private boolean fromClause = true;
                    /**
                     * If set, then ensures that the 'VARIABLES' clause within any JDOQL <code>@Query</code>s relates
                     * to a known entity type.
                     */
                    private boolean variablesClause = true;
                }
            }
        }

        private final Runtime runtime = new Runtime();
        @Data
        public static class Runtime {

            /**
             * If set, then overrides the application's {@link Locale#getDefault()}
             */
            private Optional<String> locale = Optional.empty();

            /**
             * If set, then override's the application's timezone.
             */
            private String timezone;

        }

        private final RuntimeServices runtimeServices = new RuntimeServices();
        @Data
        public static class RuntimeServices {

            private final Email email = new Email();
            @Data
            public static class Email {

                @Deprecated
                private int port = 587;

                /**
                 * The port to use for sending email.
                 *
                 * @deprecated  - ignored, instead use <code>spring.mail.port</code>
                 */
                @Deprecated
                @DeprecatedConfigurationProperty(replacement = "spring.mail.port")
                public int getPort() {
                    return port;
                }

                @Deprecated
                private int socketConnectionTimeout = 2000;

                /**
                 * The maximum number of millseconds to wait to obtain a socket connection before timing out.
                 *
                 * @deprecated  - ignored, instead use <code>spring.mail.properties.mail.smtp.connectiontimeout</code>
                 */
                @Deprecated
                @DeprecatedConfigurationProperty(replacement = "spring.mail.properties.mail.smtp.connectiontimeout")
                public int getSocketConnectionTimeout() {
                    return socketConnectionTimeout;
                }

                @Deprecated
                private int socketTimeout = 2000;

                /**
                 * The maximum number of milliseconds to wait to obtain a socket before timing out.
                 *
                 * @deprecated  - ignored, instead use <code>spring.mail.properties.mail.smtp.timeout</code>
                 */
                @Deprecated
                @DeprecatedConfigurationProperty(replacement = "spring.mail.properties.mail.smtp.timeout")
                public int getSocketTimeout() {
                    return socketTimeout;
                }

                /**
                 * If an email fails to send, whether to propagate the exception (meaning that potentially the end-user
                 * might see the exception), or whether instead to just indicate failure through the return value of
                 * the method ({@link org.apache.causeway.applib.services.email.EmailService#send(List, List, List, String, String, DataSource...)}
                 * that's being called.
                 */
                private boolean throwExceptionOnFail = true;

                private final Override override = new Override();
                @Data
                public static class Override {
                    /**
                     * Intended for testing purposes only, if set then the requested <code>to:</code> of the email will
                     * be ignored, and instead sent to this email address instead.
                     */
                    @jakarta.validation.constraints.Email
                    private String to;
                    /**
                     * Intended for testing purposes only, if set then the requested <code>cc:</code> of the email will
                     * be ignored, and instead sent to this email address instead.
                     */
                    @jakarta.validation.constraints.Email
                    private String cc;
                    /**
                     * Intended for testing purposes only, if set then the requested <code>bcc:</code> of the email will
                     * be ignored, and instead sent to this email address instead.
                     */
                    @jakarta.validation.constraints.Email
                    private String bcc;
                }

                private final Sender sender = new Sender();
                @Data
                public static class Sender {

                    @Deprecated
                    private String hostname;

                    /**
                     * Specifies the host running the SMTP service.
                     *
                     * <p>
                     *     If not specified, then the value used depends upon the email implementation.  The default
                     *     implementation will use the <code>mail.smtp.host</code> system property.
                     * </p>
                     *
                     * @deprecated - now ignored, instead use <code>spring.mail.host</code>
                     */
                    @Deprecated
                    @DeprecatedConfigurationProperty(replacement = "spring.mail.host")
                    public String getHostname() {
                        return hostname;
                    }

                    @Deprecated
                    private String username;

                    /**
                     * Specifies the username to use to connect to the SMTP service.
                     *
                     * <p>
                     *     If not specified, then the sender's {@link #getAddress() email address} will be used instead.
                     * </p>
                     *
                     * @deprecated - now ignored, instead use <code>spring.mail.username</code>
                     */
                    @Deprecated
                    @DeprecatedConfigurationProperty(replacement = "spring.mail.username")
                    public String getUsername() {
                        return username;
                    }

                    @Deprecated
                    private String password;

                    /**
                     * Specifies the password (corresponding to the {@link #getUsername() username} to connect to the
                     * SMTP service.
                     *
                     * <p>
                     *     This configuration property is mandatory (for the default implementation of the
                     *     {@link org.apache.causeway.applib.services.email.EmailService}, at least).
                     * </p>
                     *
                     * @deprecated - now ignored, instead use <code>spring.mail.password</code>
                     */
                    @Deprecated
                    @DeprecatedConfigurationProperty(replacement = "spring.mail.password")
                    public String getPassword() {
                        return password;
                    }

                    /**
                     * Specifies the email address of the user sending the email.
                     *
                     * <p>
                     *     If the {@link #getUsername() username} is not specified, is also used as the username to
                     *     connect to the SMTP service.
                     * </p>
                     *
                     * <p>
                     *     This configuration property is mandatory (for the default implementation of the
                     *     {@link org.apache.causeway.applib.services.email.EmailService}, at least).
                     * </p>
                     */
                    @jakarta.validation.constraints.Email
                    private String address;
                }

                @Deprecated
                private final Tls tls = new Tls();
                @Deprecated
                @Data
                public static class Tls {
                    @Deprecated
                    private boolean enabled = true;

                    /**
                     * Whether TLS encryption should be started (that is, <code>STARTTLS</code>).
                     *
                     * @deprecated  - now ignored, instead use <code>spring.mail.javamail.properties.mail.smtp.starttls.enable</code>
                     */
                    @Deprecated
                    @DeprecatedConfigurationProperty(replacement = "spring.mail.javamail.properties.mail.smtp.starttls.enable")
                    public boolean isEnabled() {
                        return enabled;
                    }
                }
            }

            private final ApplicationFeatures applicationFeatures = new ApplicationFeatures();
            @Data
            public static class ApplicationFeatures {
                /**
                 * Whether the {@link org.apache.causeway.applib.services.appfeat.ApplicationFeatureRepository} (or the
                 * default implementation of that service, at least) should compute the set of
                 * <code>ApplicationFeature</code> that describe the metamodel
                 * {@link ApplicationFeaturesInitConfiguration#EAGERLY eagerly}, or lazily.
                 */
                ApplicationFeaturesInitConfiguration init = ApplicationFeaturesInitConfiguration.NOT_SPECIFIED;
            }

            private final RepositoryService repositoryService = new RepositoryService();
            @Data
            public static class RepositoryService {
                private boolean disableAutoFlush = false;

                /**
                 * Normally any queries are automatically preceded by flushing pending executions.
                 *
                 * <p>
                 * This key allows this behaviour to be disabled.
                 * </p>
                 *
                 * @deprecated - use instead <code>causeway.persistence.commons.repository-service.disable-auto-flush</code>
                 */
                @Deprecated
                @DeprecatedConfigurationProperty(replacement = "causeway.persistence.commons.repository-service.disable-auto-flush")
                public boolean isDisableAutoFlush() {
                    return disableAutoFlush;
                }
            }

            private final ExceptionRecognizer exceptionRecognizer = new ExceptionRecognizer();
            @Data
            public static class ExceptionRecognizer {

                private final Dae dae = new Dae();
                @Data
                public static class Dae {
                    /**
                     * Whether the {@link org.apache.causeway.applib.services.exceprecog.ExceptionRecognizer}
                     * implementation for Spring's DataAccessException - which attempts to sanitize any exceptions
                     * arising from object stores - should be disabled (meaning that exceptions will potentially
                     * propagate as more serious to the end user).
                     */
                    private boolean disable = false;
                }
            }

            private final Translation translation = new Translation();
            @Data
            public static class Translation {

                private final Po po = new Po();

                /**
                 * Specifies the relative resource path to look for translation files.
                 * <p>
                 * If {@code null} uses {@code servletContext.getResource("/WEB-INF/")}.
                 * <p>
                 * Replaces the former Servlet context parameter 'causeway.config.dir';
                 */
                private String resourceLocation = null;

                @Data
                public static class Po {
                    /**
                     * Specifies the initial mode for obtaining/discovering translations.
                     *
                     * <p>
                     *     There are three modes:
                     *     <ul>
                     *         <li>
                     *              <p>
                     *                  The default mode of {@link Mode#WRITE write} is appropriate for
                     *                  integration testing or prototyping, meaning that the service records any requests made of it
                     *                  but just returns the string unaltered.  This is a good way to discover new strings that
                     *                  require translation.
                     *              </p>
                     *         </li>
                     *         <li>
                     *              <p>
                     *                  The {@link Mode#READ read} mode is appropriate for production; the
                     *                  service looks up translations that have previously been captured.
                     *              </p>
                     *         </li>
                     *         <li>
                     *             <p>
                     *                 The {@link Mode#DISABLED disabled} performs no translation
                     *                 and simply returns the original string unchanged.  Unlike the write mode, it
                     *                 does <i>not</i> keep track of translation requests.
                     *             </p>
                     *         </li>
                     *     </ul>
                     * </p>
                     */
                    Mode mode = Mode.WRITE;
                }
            }

            private final EntityPropertyChangePublisher entityPropertyChangePublisher = new EntityPropertyChangePublisher();

            @Data
            public static class EntityPropertyChangePublisher {

                private final Bulk bulk = new Bulk();

                @Data
                public static class Bulk {

                    /**
                     * Determines the threshold as to whether to execute a set of entity changes in blk, in other words without a transaction flush in between.
                     *
                     * <p>
                     *     If the threshold is passed (by default, anything more than 1 entity to persist), then the {@link org.apache.causeway.applib.services.repository.RepositoryService#execInBulk(Callable)} API is used.
                     * </p>
                     */
                    int threshold = 1;
                }
            }
        }
    }

    private final Persistence persistence = new Persistence();
    @Data
    public static class Persistence {

        private final Commons commons = new Commons();
        @Data
        public static class Commons {

            private final RepositoryService repositoryService = new RepositoryService();
            @Data
            public static class RepositoryService {

                /**
                 * Normally any queries are automatically preceded by flushing pending executions.
                 *
                 * <p>
                 * This key allows this behaviour to be disabled.
                 *
                 * <p>
                 *     NOTE: this key is redundant for JPA/EclipseLink, which supports its own auto-flush using
                 *     <a href="https://www.eclipse.org/eclipselink/documentation/2.7/jpa/extensions/persistenceproperties_ref.htm#BABDHEEB">eclipselink.persistence-context.flush-mode</a>
                 * </p>
                 */
                private boolean disableAutoFlush = false;
            }

            private final EntityChangeTracker entityChangeTracker = new EntityChangeTracker();
            @Data
            public static class EntityChangeTracker {
                /**
                 * Normally any query submitted to {@link org.apache.causeway.applib.services.repository.RepositoryService#allMatches(Query)} will trigger a
                 * flush first, unless auto-flush has been {@link Core.RuntimeServices.RepositoryService#isDisableAutoFlush() disabled}.
                 *
                 * <p>
                 *     However, this auto-flush behaviour can be troublesome if the query occurs as a side-effect of the evaluation of a derived property,
                 *     whose value in turn is enlisted by an implementation of a subscriber (in particular {@link EntityPropertyChangeSubscriber}) which
                 *     captures the value of all properties (both persisted and derived).  However, this behaviour can (at least under JDO), result in a {@link java.util.ConcurrentModificationException}.
                 * </p>
                 *
                 * <p>
                 *     By default, {@link EntityChangeTracker} will therefore temporarily suppress any auto-flushing while this is ongoing.  The purpose
                 *     of this configuration property is to never suppress, ie always autoflush.
                 * </p>
                 */
                private boolean suppressAutoFlush = true;
                /**
                 * Provides a mechanism to globally enable or disable this service.
                 *
                 * <p>
                 *     By default this service is enabled (if added to the classpath as a module).
                 * </p>
                 */
                private boolean enabled = true;
            }
        }

        private final Schema schema = new Schema();
        @Data
        public static class Schema {

            /**
             * List of additional schemas to be auto-created.
             * <p>
             * Explicitly creates given list of schemas by using the specified
             * {@link #getCreateSchemaSqlTemplate()} to generate the actual SQL
             * statement against the configured data-source.
             * <p>
             * This configuration mechanism does not consider any schema-auto-creation
             * configuration (if any), that independently is provided the standard JPA way.
             */
            private final List<String> autoCreateSchemas = new ArrayList<>();

            /**
             * Does lookup additional "mapping-files" in META-INF/orm-<i>name</i>.xml
             * (equivalent to "mapping-file" entries in persistence.xml) and adds these
             * to those that are already configured the <i>Spring Data</i> way (if any).
             * @implNote not implemented for JDO
             */
            private final List<String> additionalOrmFiles = new ArrayList<>();

            /**
             * Vendor specific SQL syntax to create a DB schema.
             * <p>
             * This template is passed through {@link String#format(String, Object...)} to
             * make the actual SQL statement thats to be used against the configured data-source.
             * <p>
             * Default template is {@literal CREATE SCHEMA IF NOT EXISTS %S} with the schema name
             * converted to upper-case.
             * <p>
             * For MYSQL/MARIADB use escape like {@code `%S`}
             */
            private String createSchemaSqlTemplate = "CREATE SCHEMA IF NOT EXISTS %S";

        }
    }

    private final Prototyping prototyping = new Prototyping();
    @Data
    public static class Prototyping {

        private final H2Console h2Console = new H2Console();
        @Data
        public static class H2Console {
            /**
             * Whether to allow remote access to the H2 Web-Console,
             * which is a potential security risk when no web-admin password is set.
             * <p>
             * Corresponds to Spring Boot's "spring.h2.console.settings.web-allow-others" config property.
             */
            private boolean webAllowRemoteAccess = false;

            /**
             * Whether to generate a random password for access to the H2 Web-Console advanced features.
             *
             * <p>
             * If a password is generated, it is logged to the logging subsystem (Log4j2).
             * </p>
             *
             * <p>
             * Recommended (<code>true</code>) when {@link #isWebAllowRemoteAccess()} is also <code>true</code>.
             * </p>
             */
            private boolean generateRandomWebAdminPassword = true;
        }

        public enum IfHiddenPolicy {
            /**
             * The default  behaviour: any properties, collections or actions whose visibility has been vetoed
             * will not be shown in the UI.
             */
            HIDE,
            /**
             * To assist with the debugging security and similar: any properties, collections or actions whose
             * visibility has been vetoed will instead be shown as merely disabled.
             */
            SHOW_AS_DISABLED,
            /**
             * To assist with the debugging security and similar: any properties, collections or actions whose
             * visibility has been vetoed will instead be shown as merely disabled, and in addition the tooltips will
             * indicate the class name of the facet/advisor(s) that did the vetoing.
             */
            SHOW_AS_DISABLED_WITH_DIAGNOSTICS;
            public boolean isHide() { return this == HIDE; }
            public boolean isShowAsDisabled() { return this == SHOW_AS_DISABLED; }
            public boolean isShowAsDisabledWithDiagnostics() { return this == SHOW_AS_DISABLED_WITH_DIAGNOSTICS; }
        }

        /**
         * Whether and how to display any properties, actions and collections whose visibility has been vetoed.
         *
         * <p>
         *     By default, such object members are of course hidden.  However, this config property can be used to
         *     instead show these objects as disabled, with the tooltip indicating why the object member has been
         *     vetoed.  Setting the property to
         *     {@link IfHiddenPolicy#SHOW_AS_DISABLED_WITH_DIAGNOSTICS SHOW_AS_DISABLED_WITH_DIAGNOSTICS} shows
         *     additional detail in the tooltip.
         * </p>
         *
         * <p>
         *     This config property only applies in prototyping mode.
         * </p>
         */
        private IfHiddenPolicy ifHiddenPolicy = IfHiddenPolicy.HIDE;

        public enum IfDisabledPolicy {
            /**
             * The default  behaviour: any properties, collections or actions whose usability has been vetoed
             * will be shown as disabled in the UI.
             */
            DISABLE,
            /**
             * To assist with the debugging security and similar: any properties, collections or actions whose
             * usability has been vetoed will continue to be shown as disabled, and in addition the tooltips will
             * indicate the class name of the facet/advisor(s) that did the vetoing.
             */
            SHOW_AS_DISABLED_WITH_DIAGNOSTICS;
            public boolean isDisabled() { return this == DISABLE; }
            public boolean isShowAsDisabledWithDiagnostics() { return this == SHOW_AS_DISABLED_WITH_DIAGNOSTICS; }
        }

        /**
         * Whether and how to display any properties, actions and collections whose usability has been vetoed.
         *
         * <p>
         *     By default, such object members are shown as disabled, with the tooltip indicating why. Setting the
         *     property to {@link IfDisabledPolicy#SHOW_AS_DISABLED_WITH_DIAGNOSTICS SHOW_AS_DISABLED_WITH_DIAGNOSTICS} shows additional detail in the
         *     tooltip.
         * </p>
         *
         * <p>
         *     This config property only applies in prototyping mode.
         * </p>
         */
        private IfDisabledPolicy ifDisabledPolicy = IfDisabledPolicy.DISABLE;
    }

    private final Viewer viewer = new Viewer();
    @Data
    public static class Viewer {

        private final Common common = new Common();
        @Data
        public static class Common {

            private final Application application = new Application();
            @Data
            public static class Application {

                /**
                 * Label used on the about page.
                 */
                private String about;

                /**
                 * Either the location of the image file (relative to the class-path resource root),
                 * or an absolute URL.
                 *
                 * <p>
                 * This is rendered on the header panel. An image with a size of 160x40 works well.
                 * If not specified, the application.name is used instead.
                 * </p>
                 */
                @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
                private Optional<String> brandLogoHeader = Optional.empty();

                /**
                 * Either the location of the image file (relative to the class-path resource root),
                 * or an absolute URL.
                 *
                 * <p>
                 * This is rendered on the sign-in page. An image with a size of 400x40 works well.
                 * If not specified, the {@link Application#getName() application name} is used instead.
                 * </p>
                 */
                @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
                private Optional<String> brandLogoSignin = Optional.empty();

                /**
                 * Specifies the URL to use of the favIcon.
                 *
                 * <p>
                 *     This is expected to be a local resource.
                 * </p>
                 */
                @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
                private Optional<String> faviconUrl = Optional.empty();

                /**
                 * Specifies the file name containing the menubars.
                 *
                 * <p>
                 *     This is expected to be a local resource.
                 * </p>
                 */
                @NotNull @NotEmpty
                private String menubarsLayoutFile = "menubars.layout.xml";

                /**
                 * Identifies the application on the sign-in page
                 * (unless a {@link Application#brandLogoSignin sign-in} image is configured) and
                 * on top-left in the header
                 * (unless a {@link Application#brandLogoHeader header} image is configured).
                 */
                @NotNull @NotEmpty
                private String name = "Apache Causeway ™";

                /**
                 * The version of the application, eg 1.0, 1.1, etc.
                 *
                 * <p>
                 * If present, then this will be shown in the footer on every page as well as on the
                 * about page.
                 * </p>
                 */
                private String version;
            }

            /**
             * List of organisations or individuals to give credit to, shown as links and icons in the footer.
             * A maximum of 3 credits can be specified.
             *
             * <p>
             * IntelliJ unfortunately does not provide IDE completion for lists of classes; YMMV.
             * </p>
             *
             * <p>
             * @implNote - For further discussion, see for example
             * <a href="https://stackoverflow.com/questions/41417933/spring-configuration-properties-metadata-json-for-nested-list-of-objects">this stackoverflow question</a>
             * and <a href="https://github.com/spring-projects/spring-boot/wiki/IDE-binding-features#simple-pojo">this wiki page</a>.
             * </p>
             */
            private List<Credit> credit = new ArrayList<>();

            @Data
            public static class Credit {
                /**
                 * URL of an organisation or individual to give credit to, appearing as a link in the footer.
                 *
                 * <p>
                 *     For the credit to appear, the {@link #getUrl() url} must be provided along with either
                 *     {@link #getName() name} and/or {@link #getImage() image}.
                 * </p>
                 */
                @jakarta.validation.constraints.Pattern(regexp="^http[s]?://[^:]+?(:\\d+)?.*$")
                private String url;
                /**
                 * URL of an organisation or individual to give credit to, appearing as text in the footer.
                 *
                 * <p>
                 *     For the credit to appear, the {@link #getUrl() url} must be provided along with either
                 *     {@link #getName() name} and/or {@link #getImage() image}.
                 * </p>
                 */
                private String name;
                /**
                 * Name of an image resource of an organisation or individual, appearing as an icon in the footer.
                 *
                 * <p>
                 *     For the credit to appear, the {@link #getUrl() url} must be provided along with either
                 *     {@link #getName() name} and/or {@link #getImage() image}.
                 * </p>
                 */
                @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
                private String image;

                /**
                 * Whether enough information has been defined for the credit to be appear.
                 */
                public boolean isDefined() { return (name != null || image != null) && url != null; }
            }
        }

        private final Graphql graphql = new Graphql();
        @Data
        public static class Graphql {

            /**
             * Which style of schema to expose: &quot;simple&quot;, &quot;rich&quot; or some combination of both.
             *
             * @since 2.0 {@index}
             */
            public enum SchemaStyle {
                /**
                 * Expose only the &quot;simple&quot; schema, defining only fields that return the state of the domain
                 * objects but with no fields to represent additional facets of state (such as whether
                 * an action is hidden or disabled).
                 *
                 * <p>
                 *     Suitable for clients where the application logic and state is the responsibility of the client.
                 * </p>
                 */
                SIMPLE_ONLY,
                /**
                 * Expose only the &quot;rich&quot; schema, exposing not only fields that return the state of the domain
                 * objects but <i>also</i> with fields to represent additional facets of state (such as whether
                 * an action is hidden or disabled).
                 *
                 * <p>
                 *     Optionally, fields for Scenario (given/when/then) testing may also be added if the
                 *     {@link Schema.Rich#isEnableScenarioTesting()} config property is set.
                 * </p>
                 * <p>
                 *     Suitable for clients where the application logic and state remains in the backend, within the
                 *     domain model hosted by Causeway.
                 * </p>
                 */
                RICH_ONLY,
                /**
                 * Exposes both the simple and rich schemas, for the query have each under a field as defined by
                 * {@link Schema.Simple#getTopLevelFieldName()} (by default &quot;simple&quot;) and
                 * {@link Schema.Rich#getTopLevelFieldName()} (by default &quot;rich&quot;).
                 *
                 * <p>
                 *     For mutations, use the <i>simple</i> schema types.
                 * </p>
                 */
                SIMPLE_AND_RICH,
                /**
                 * Exposes both the simple and rich schemas, for the query have each under a field as defined by
                 * {@link Schema.Simple#getTopLevelFieldName()} (by default &quot;simple&quot;) and
                 * {@link Schema.Rich#getTopLevelFieldName()} (by default &quot;rich&quot;).
                 *
                 * <p>
                 *     For mutations, use the <i>rich</i> schema types.
                 * </p>
                 */
                RICH_AND_SIMPLE,
                ;

                public boolean isRich() {
                    return this == RICH_ONLY || this == SIMPLE_AND_RICH || this == RICH_AND_SIMPLE;
                }
                public boolean isSimple() {
                    return this == SIMPLE_ONLY || this == SIMPLE_AND_RICH || this == RICH_AND_SIMPLE;
                }
            }

            /**
             * Which {@link SchemaStyle} to expose, &quot;simple&quot; or &quot;rich&quot;.  By default both are
             * exposed under top-level field names.
             *
             * @see Schema.Rich#getTopLevelFieldName()
             * @see Schema.Simple#getTopLevelFieldName()
             */
            private SchemaStyle schemaStyle = SchemaStyle.RICH_AND_SIMPLE;

            private final Schema schema = new Schema();
            @Data
            public static class Schema {

                private final Simple simple = new Simple();
                @Data
                public static class Simple {

                    /**
                     * If the {@link #getSchemaStyle()} is set to {@link SchemaStyle#SIMPLE_AND_RICH}, defines the name of the
                     * top-level field under which the &quot;simple&quot; schema resides.
                     *
                     * <p>
                     *     Ignored for any other {@link #getSchemaStyle()}.
                     * </p>
                     */
                    private String topLevelFieldName = "simple";
                }

                private final Rich rich = new Rich();
                @Data
                public static class Rich {

                    /**
                     * If the {@link #getSchemaStyle()} is set to {@link SchemaStyle#SIMPLE_AND_RICH}, defines the name of the
                     * top-level field under which the &quot;rich&quot; schema resides.
                     *
                     * <p>
                     *     Ignored for any other {@link #getSchemaStyle()}.
                     * </p>
                     */
                    private String topLevelFieldName = "rich";

                    /**
                     * If the {@link #getSchemaStyle()} is set to either {@link SchemaStyle#RICH_ONLY} or
                     * {@link SchemaStyle#SIMPLE_AND_RICH}, then determines whether the &quot;Scenario&quot; field is included
                     * in order to allow given/when/then tests to be expressed.
                     *
                     * <p>
                     *     Ignored if the {@link #getSchemaStyle()} is {@link SchemaStyle#SIMPLE_ONLY}.
                     * </p>
                     */
                    private boolean enableScenarioTesting = false;
                }
            }

            public enum ApiVariant {
                /**
                 * Exposes only a Query API, of properties, collections and safe (query-only) actions.
                 * Any actions that mutate the state of the system (in other words are idempotent or non-idempotent)
                 * are excluded from the API, as is the ability to set properties.
                 */
                QUERY_ONLY,
                /**
                 * Exposes an API with Query for query/safe separate queries and field access, with mutating (idempotent
                 * and non-idempotent) actions and property setters instead surfaced as Mutations, as per the
                 * <a href="https://spec.graphql.org/June2018/#sec-Language.Operations">GraphQL spec</a>.
                 */
                QUERY_AND_MUTATIONS,
                /**
                 * Exposes an API with both Query and Mutations, but relaxes the constraints for the Query API by also
                 * including idempotent and non-idempotent actions and property setters.
                 *
                 * <p>
                 *     <b>IMPORTANT</b>: be aware that the resultant API is not compliant with the rules of the
                 *     GraphQL spec; in particular, it violates
                 *     <a href="https://spec.graphql.org/June2018/#sec-Language.Operations">2.3 Operations</a> which
                 *     states: &quot;query – [is] a read‐only fetch.&quot;
                 * </p>
                 */
                QUERY_WITH_MUTATIONS_NON_SPEC_COMPLIANT,
                ;
            }

            /**
             * Which variant of API to expose: {@link ApiVariant#QUERY_ONLY} (which suppresses any actions that mutate the state of the
             * system), or as {@link ApiVariant#QUERY_AND_MUTATIONS} (which additionally exposes actions that mutate the system as mutations)
             * or alternatively as {@link ApiVariant#QUERY_WITH_MUTATIONS_NON_SPEC_COMPLIANT}, a query-only schema that relaxes the read-only rule
             * by exposing actions that mutate the system; it is therefore not compliant with the GraphQL spec),
             */
            private ApiVariant apiVariant = ApiVariant.QUERY_AND_MUTATIONS;

            /**
             * Specifies which elements of the metamodel are included within the generated
             * GraphQL spec.
             *
             * @since 2.x {@index}
             */
            public enum ApiScope {

                /**
                 * The generated GraphQL spec is restricted only to include only
                 * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL view model}s.
                 *
                 * <p>
                 * Applicable when the GraphQL API is in use by third-party clients, ie public use and not
                 * under the control of the authors of the backend Apache Causeway application.
                 * Exposing entities also would couple the GraphQL client too deeply to the backend implementation.
                 * </p>
                 */
                VIEW_MODELS,
                /**
                 * The generated GraphQL spec is not restricted, includes both
                 * {@link org.apache.causeway.applib.annotation.Nature#ENTITY domain entities} as well as
                 * {@link org.apache.causeway.applib.annotation.Nature#VIEW_MODEL view model}s.
                 *
                 * <p>
                 * This is perfectly acceptable where the team developing the GraphQL client is the
                 * same as the team developing the backend service ... the use of the Web
                 * API between the client and server is a private implementation detail of
                 * the application.
                 * </p>
                 */
                ALL,
                ;
            }

            /**
             * Which domain objects to include the GraphQL schema.  By default, all domain objects are exposed
             * (entities and view models).
             */
            private ApiScope apiScope = ApiScope.ALL;

            private final MetaData metaData = new MetaData();
            @Data
            public static class MetaData {
                /**
                 * Note that field names <i>cannot</i> being with &quot;__&quot;, as that is reserved by the
                 * underlying GraphQL implementation.
                 */
                private String fieldName = "_meta";
            }

            private final Lookup lookup = new Lookup();
            @Data
            public static class Lookup {
                /**
                 * Lookup field prefix
                 */
                private String fieldNamePrefix = "";
                /**
                 * Lookup field suffix
                 */
                private String fieldNameSuffix = "";
                /**
                 * This is the name of the synthetic first argument used to locate the object to be looked up.
                 */
                private String argName = "object";
            }

            private final Mutation mutation = new Mutation();
            @Data
            public static class Mutation {
                /**
                 * The name of the synthetic argument of mutators representing the target domain object.
                 */
                private String targetArgName = "_target";
            }

            private final ScalarMarshaller scalarMarshaller = new ScalarMarshaller();
            @Data
            public static class ScalarMarshaller {

                /**
                 * For both JDK8's {@link java.time.LocalTime} and JodaTime's {@link org.joda.time.LocalTime}
                 */
                private String localTimeFormat = "HH:mm:ss";
                /**
                 * For both JDK8's {@link java.time.LocalDate} and JodaTime's {@link org.joda.time.LocalDate}
                 */
                private String localDateFormat = "yyyy-MM-dd";
                /**
                 * for JDK8's {@link java.time.ZonedDateTime} and JodaTime's {@link org.joda.time.DateTime}
                 */
                private String zonedDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
            }

            /**
             * The different ways in which resources ({@link org.apache.causeway.applib.value.Blob} bytes,
             * {@link org.apache.causeway.applib.value.Clob} chars, grids and icons) can be downloaded from the
             * resource controller.
             */
            public enum ResponseType {
                /**
                 * Do not allow the resources to be downloaded at all.  This is the default.
                 *
                 * <p>
                 *     In this case any {@link org.apache.causeway.applib.value.Blob} and
                 *     {@link org.apache.causeway.applib.value.Clob} properties will <i>not</i> provide a link to
                 *     the URL.  Attempting to download from the resource controller will result in a 403 (forbidden).
                 * </p>
                 */
                FORBIDDEN,
                /**
                 * Allows resources to be downloaded directly.
                 *
                 * <p>
                 *     <b>IMPORTANT: </b> if enabling this configuration property, make sure that the <code>ResourcesController</code> endpoints
                 *     are secured appropriately.
                 * </p>
                 */
                DIRECT,
                /**
                 * Allows resources to be downloaded as attachments (using <code>Content-Disposition</code> header).
                 *
                 * <p>
                 *     <b>IMPORTANT: </b> if enabling this configuration property, make sure that the <code>ResourcesController</code> endpoints
                 *     are secured appropriately.
                 * </p>
                 */
                ATTACHMENT,
                ;
            }

            @Getter
            private final Resources resources = new Resources();
            @Data
            public static class Resources {
                /**
                 * How resources ({@link org.apache.causeway.applib.value.Blob} bytes,
                 * {@link org.apache.causeway.applib.value.Clob} chars, grids and icons) can be downloaded from the
                 * resource controller.
                 *
                 * <p>
                 *     By default the download of these resources if {@link ResponseType#FORBIDDEN}, but alternatively
                 *     they can be enabled to download either {@link ResponseType#DIRECT}ly or as an
                 *     {@link ResponseType#ATTACHMENT}.
                 * </p>
                 */
                private ResponseType responseType = ResponseType.FORBIDDEN;
            }

            @Getter
            private final Authentication authentication = new Authentication();
            @Data
            public static class Authentication {

                @Getter
                private final Fallback fallback = new Fallback();
                @Data
                public static class Fallback {

                    /**
                     * Used as the default username (if not provided by other means).
                     */
                    private String username;

                    /**
                     * Used as the set of roles for the default {@link #getUsername() username} (if not provided by other means).
                     */
                    private List<String> roles;
                }
            }
        }

        private final Restfulobjects restfulobjects = new Restfulobjects();
        @Data
        public static class Restfulobjects {

            @Getter
            private final Authentication authentication = new Authentication();
            @Data
            public static class Authentication {
                /**
                 * Defaults to <code>org.apache.causeway.viewer.restfulobjects.viewer.webmodule.auth.AuthenticationStrategyBasicAuth</code>.
                 */
                private String strategyClassName = "org.apache.causeway.viewer.restfulobjects.viewer.webmodule.auth.AuthenticationStrategyBasicAuth";
            }

            /**
             * Whether to enable the <code>x-ro-follow-links</code> support, to minimize round trips.
             *
             * <p>
             *     The RO viewer provides the capability for the client to set the optional
             *     <code>x-ro-follow-links</code> query parameter, as described in section 34.4 of the RO spec v1.0.
             *     If used, the resultant representation includes the result of following the associated link, but
             *     through a server-side "join", somewhat akin to GraphQL.
             * </p>
             *
             * <p>
             *     By default this functionality is disabled, this configuration property enables the feature.
             *     If enabled, then the representations returned are non-standard with respect to the RO Spec v1.0.
             * </p>
             */
            private boolean honorUiHints = false;

            /**
             * When rendering domain objects, if set the representation returned is stripped back to a minimal set,
             * excluding links to actions and collections and with a simplified representation of an object's
             * properties.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean objectPropertyValuesOnly = false;

            /**
             * If set, then any unrecognised <code>Accept</code> headers will result in an HTTP <i>Not Acceptable</i>
             * response code (406).
             */
            private boolean strictAcceptChecking = false;

            /**
             * If set, then the representations returned will omit any links to the formal domain-type representations.
             */
            private boolean suppressDescribedByLinks = false;

            /**
             * If set, then - should there be an interaction with an action, property or collection that is disabled -
             * then this will prevent the <code>disabledReason</code> reason from being added to the returned
             * representation.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean suppressMemberDisabledReason = false;

            /**
             * If set, then the <code>x-causeway-format</code> key (under <code>extensions</code>) for properties will be
             * suppressed.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean suppressMemberExtensions = false;

            /**
             * If set, then the <code>id</code> key for all members will be suppressed.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean suppressMemberId = false;

            /**
             * If set, then the detail link (in other words <code>links[rel='details' ...]</code>) for all members
             * will be suppressed.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean suppressMemberLinks = false;

            /**
             * If set, then the update link (in other words <code>links[rel='update'... ]</code> to perform a bulk
             * update of an object) will be suppressed.
             *
             * <p>
             *     This is disabled by default.  If enabled, then the representations returned are non-standard with
             *     respect to the RO Spec v1.0.
             * </p>
             */
            private boolean suppressUpdateLink = false;

            /**
             * If left unset (the default), then the RO viewer will use the {@link jakarta.ws.rs.core.UriInfo}
             * (injected using {@link jakarta.ws.rs.core.Context}) to figure out the base Uri (used to render
             * <code>href</code>s).
             *
             * <p>
             * This will be correct much of the time, but will almost certainly be wrong if there is a reverse proxy.
             * </p>
             *
             * <p>
             * If set, eg <code>https://dev.myapp.com/</code>, then this value will be used instead.
             * </p>
             */
            @jakarta.validation.constraints.Pattern(regexp="^http[s]?://[^:]+?(:\\d+)?/([^/]+/)*+$")
            private Optional<String> baseUri = Optional.empty();
        }

        private final Wicket wicket = new Wicket();
        @Data
        public static class Wicket {

            /**
             * Whether actions, that have explicit <code>hidden = Where</code> semantics
             * to enable them in tables,
             * should be gathered into an action column.
             * That is, collections of domain objects are presented in the UI as tables,
             * where corresponding domain object actions are gathered into an additional
             * (typically trailing) column (labeled 'action-column').
             */
            private boolean actionColumnEnabled = true;

            /**
             * Whether actions, that on click will show a dialog,
             * should be indicated by a trailing ellipsis on the action's label.
             * <p>
             * Applies to both, action buttons and action menu items.
             */
            private boolean actionIndicationWhenBoundToDialog = true;

            /**
             * Specifies the subclass of
             * <code>org.apache.causeway.viewer.wicket.viewer.wicketapp.CausewayWicketApplication</code> that is used to
             * bootstrap Wicket.
             *
             * <p>
             *     There is usually very little reason to change this from its default.
             * </p>
             */
            private String app = "org.apache.causeway.viewer.wicket.viewer.wicketapp.CausewayWicketApplication";

            /**
             * Whether the Ajax debug should be shown, by default this is disabled.
             */
            private boolean ajaxDebugMode = false;

            /**
             * The base path at which the Wicket viewer is mounted.
             */
            @jakarta.validation.constraints.Pattern(regexp="^[/](.*[/]|)$") @NotNull @NotEmpty
            private String basePath = "/wicket/";

            /**
             * If the end user uses a deep link to access the Wicket viewer, but is not authenticated, then this
             * configuration property determines whether to continue through to that original destination once
             * authenticated, or simply to go to the home page.
             *
             * <p>
             *     The default behaviour is to honour the original destination requested.
             * </p>
             */
            private boolean clearOriginalDestination = false;

            /**
             * Whether the clear-field-button, that allows to clear a null-able (optional) field
             * (a property or a dialog argument) is enabled for rendering or not.
             *
             * <p>
             *     The default is to enable (show) the clear-field-button.
             * </p>
             */
            private boolean clearFieldButtonEnabled = true;

            /**
             * In prototyping mode, a text icon is appended to any property that is disabled, with its tool-tip explaining why the property is disabled.
             * This configuration property can be used to suppress the icon, even in prototyping mode, if desired.
             *
             * <p>
             *     The default is to enable (show) the text icon (if in prototyping mode).
             * </p>
             */
            private boolean disableReasonExplanationInPrototypingModeEnabled = true;

            /**
             * URL of file to read any custom CSS, relative to <code>static</code> package on the class path.
             *
             * <p>
             *     A typical value is <code>css/application.css</code>.  This will result in this file being read
             *     from the <code>static/css</code> directory (because static resources such as CSS are mounted by
             *     Spring by default under <code>static</code> package).
             * </p>
             */
            @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
            private Optional<String> css = Optional.empty();

            /**
             * Whether the dialog mode rendered when invoking actions on domain objects should be to use
             * the sidebar (the default) or to use a modal dialog.
             *
             * <p>
             *     This can be overridden on a case-by-case basis using {@link ActionLayout#promptStyle()}.
             * </p>
             */
            private DialogMode dialogMode = DialogMode.SIDEBAR;

            /**
             * Whether the dialog mode rendered when invoking actions on domain services (that is, menus) should be to
             * use a modal dialog (the default) or to use the sidebar panel.
             *
             * <p>
             *     This can be overridden on a case-by-case basis using {@link ActionLayout#promptStyle()}.
             * </p>
             */
            private DialogMode dialogModeForMenu = DialogMode.MODAL;

            /**
             * URL of file to read any custom JavaScript, relative to <code>static</code> package on the class path.
             *
             * <p>
             *     A typical value is <code>js/application.js</code>.  This will result in this file being read
             *     from the <code>static/js</code> directory (because static resources such as CSS are mounted by
             *     Spring by default under <code>static</code> package).
             * </p>
             */
            @jakarta.validation.constraints.Pattern(regexp="^[^/].*$")
            private Optional<String> js = Optional.empty();

            /**
             * If specified, then is rendered on each page to enable live reload.
             *
             * <p>
             *     Configuring live reload also requires an appropriate plugin to the web browser (eg see
             *     <a href="http://livereload.com/">livereload.com</a> and a mechanism to trigger changes, eg by
             *     watching <code>Xxx.layout.xml</code> files.
             * </p>
             */
            private Optional<String> liveReloadUrl = Optional.empty();

            /**
             * The maximum number of characters to use to render the title of a domain object (alongside the icon)
             * in any table, if not otherwise overridden by either {@link #getMaxTitleLengthInParentedTables()}
             * or {@link #getMaxTitleLengthInStandaloneTables()}.
             *
             * <p>
             *     If truncated, then the remainder of the title will be replaced with ellipses (...).
             * </p>
             */
            private int maxTitleLengthInTables = 12;

            private static boolean isMaxTitleLenghtValid(final int len) {
                return len>=0;
            }
            private int asTitleLenght(final int len) {
                return isMaxTitleLenghtValid(len)
                        ? len
                        : getMaxTitleLengthInTables();
            }

            private int maxTitleLengthInParentedTables = -1;

            /**
             * The maximum number of characters to use to render the title of a domain object (alongside the icon) in a
             * parented table.
             *
             * <p>
             *     If truncated, then the remainder of the title will be replaced with ellipses (...).
             * </p>
             *
             * <p>
             *     If invalid or not specified, then the value of {@link #getMaxTitleLengthInTables()} is used.
             * </p>
             */
            public int getMaxTitleLengthInParentedTables() {
                return asTitleLenght(maxTitleLengthInParentedTables);
            }

            public void setMaxTitleLengthInParentedTables(final int val) {
                maxTitleLengthInParentedTables = val;
            }

            private int maxTitleLengthInStandaloneTables = -1;

            /**
             * The maximum number of characters to use to render the title of a domain object (alongside the icon)
             * in a standalone table, ie the result of invoking an action.
             *
             * <p>
             *     If truncated, then the remainder of the title will be replaced with ellipses (...).
             * </p>
             *
             * <p>
             *     If invalid or not specified, then the value of {@link #getMaxTitleLengthInTables()} is used.
             * </p>
             */
            public int getMaxTitleLengthInStandaloneTables() {
                return asTitleLenght(maxTitleLengthInStandaloneTables);
            }
            /**
             * The maximum length that a title of an object will be shown when rendered in a standalone table;
             * will be truncated beyond this (with ellipses to indicate the truncation).
             * <p>
             * If set to negative or zero, sets defaults instead.
             */
            public void setMaxTitleLengthInStandaloneTables(final int val) {
                maxTitleLengthInStandaloneTables = val;
            }

            /**
             * If a table has no property columns,
             * for the title column this value is used,
             * to determine how many characters to render for the table element titles.
             * <p>
             * Introduced for the case when max-title-length is set to zero for tables in general,
             * that if a table has no property columns an exception to that title suppression can be made.
             */
            private int maxTitleLengthInTablesNotHavingAnyPropertyColumn = 80;

            /**
             * Whether to use a modal dialog for property edits and for actions associated with properties.
             *
             * <p>
             * This can be overridden on a case-by-case basis using <code>@PropertyLayout#promptStyle</code> and
             * <code>@ActionLayout#promptStyle</code>.
             * </p>
             *
             * <p>
             * This behaviour is disabled by default; the viewer will use an inline prompt in these cases, making for a smoother
             * user experience.
             * </p>
             */
            private PromptStyle promptStyle = PromptStyle.INLINE;

            /**
             * Whether to redirect to a new page, even if the object being shown (after an action invocation or a property edit)
             * is the same as the previous page.
             *
             * <p>
             * This behaviour is disabled by default; the viewer will update the existing page if it can, making for a
             * smoother user experience. If enabled then this reinstates the pre-1.15.0 behaviour of redirecting in all cases.
             * </p>
             */
            private boolean redirectEvenIfSameObject = false;

            /**
             * In Firefox and more recent versions of Chrome 54+, cannot copy out of disabled fields; instead we use the
             * readonly attribute (https://www.w3.org/TR/2014/REC-html5-20141028/forms.html#the-readonly-attribute)
             *
             * <p>
             * This behaviour is enabled by default but can be disabled using this flag
             * </p>
             */
            private boolean replaceDisabledTagWithReadonlyTag = true;

            /**
             * Whether to disable a form submit button after it has been clicked, to prevent users causing an error if they
             * do a double click.
             *
             * This behaviour is enabled by default, but can be disabled using this flag.
             */
            private boolean preventDoubleClickForFormSubmit = true;

            /**
             * Whether to disable a no-arg action button after it has been clicked, to prevent users causing an error if they
             * do a double click.
             *
             * <p>
             * This behaviour is enabled by default, but can be disabled using this flag.
             * </p>
             */
            private boolean preventDoubleClickForNoArgAction = true;

            /**
             * Whether to show the footer menu bar.
             *
             * <p>
             *     This is enabled by default.
             * </p>
             */
            private boolean showFooter = true;

            /**
             * Whether Wicket tags should be stripped from the markup.
             *
             * <p>
             *      By default this is enabled, in other words Wicket tags are stripped.  Please be aware that if
             *      tags are <i>not</i> stripped, then this may break CSS rules on some browsers.
             * </p>
             */
            private boolean stripWicketTags = true;

            /**
             * Whether to suppress the sign-up link on the sign-in page.
             *
             * <p>
             *     Although this is disabled by default (in other words the sign-up link is not suppressed), note that
             *     in addition the application must provide an implementation of the
             *     {@link UserRegistrationService} as well as a
             *     configured {@link EmailNotificationService} (same conditions
             *     as for the {@link #isSuppressPasswordReset()} password reset link).
             * </p>
             */
            private boolean suppressSignUp = false;

            /**
             * Whether to suppress the password reset link on the sign-in page.
             *
             * <p>
             *     Although this is disabled by default (in other words the 'reset password' link is not suppressed),
             *     note that in addition the application must provide an implementation of the
             *     {@link UserRegistrationService} as well as a
             *     configured {@link EmailNotificationService} (same conditions
             *     as for the {@link #isSuppressSignUp()} sign-up link).
             * </p>
             */
            private boolean suppressPasswordReset = false;

            /**
             * How to interpret tooltip content, e.g. as HTML or TEXT. default = TEXT
             */
            private TextMode tooltipTextMode = TextMode.defaults();

            /**
             * Whether to show an indicator for a form submit button that it has been clicked.
             *
             * <p>
             * This behaviour is enabled by default.
             * </p>
             */
            private boolean useIndicatorForFormSubmit = true;

            /**
             * Whether to show an indicator for a no-arg action button that it has been clicked.
             *
             * <p>
             * This behaviour is enabled by default.
             * </p>
             */
            private boolean useIndicatorForNoArgAction = true;

            /**
             * Whether to show an indicator for a sortable column (that is an up-down arrow icon).
             *
             * <p>
             * This behaviour is disabled by default.
             * </p>
             */
            private boolean useIndicatorForSortableColumn = false;

            /**
             * Whether the Wicket source plugin should be enabled; if so, the markup includes links to the Wicket source.
             *
             * <p>
             *     This behaviour is disabled by default.  Please be aware that enabling it can substantially impact
             *     performance.
             * </p>
             */
            private boolean wicketSourcePlugin = false;

            private final BookmarkedPages bookmarkedPages = new BookmarkedPages();
            @Data
            public static class BookmarkedPages {

                /**
                 * Whether the panel providing linsk to previously visited object should be accessible from the top-left of the header.
                 */
                private boolean showChooser = true;

                /**
                 * Specifies the maximum number of bookmarks to show.
                 *
                 * <p>
                 *     These are aged out on an MRU-LRU basis.
                 * </p>
                 */
                private int maxSize = 15;

                /**
                 * Whether the drop-down list of previously visited objects should be shown in the footer.
                 */
                private boolean showDropDownOnFooter = true;

            }

            private final Breadcrumbs breadcrumbs = new Breadcrumbs();
            @Data
            public static class Breadcrumbs {
                /**
                 * Whether to enable the 'where am i' feature, in other words the breadcrumbs.
                 */
                private boolean enabled = true;
                /**
                 *
                 */
                private int maxParentChainLength = 64;
            }

            private final DatePicker datePicker = new DatePicker();
            @Data
            public static class DatePicker {

                /**
                 * Defines the first date available in the date picker.
                 * <p>
                 * As per http://eonasdan.github.io/bootstrap-datetimepicker/Options/#maxdate, in ISO format (per https://github.com/moment/moment/issues/1407).
                 * <p>
                 * Use time zone 'Z', as the date/time picker UI component is not wired up to support time-zones.
                 */
                @NotEmpty @NotNull
                private String minDate = "1900-01-01T00:00:00.000Z";

                /**
                 * Defines the first date available in the date picker.
                 * <p>
                 * As per http://eonasdan.github.io/bootstrap-datetimepicker/Options/#maxdate, in ISO format (per https://github.com/moment/moment/issues/1407).
                 * <p>
                 * Use time zone 'Z', as the date/time picker UI component is not wired up to support time-zones.
                 */
                @NotEmpty @NotNull
                private String maxDate = "2100-01-01T00:00:00.000Z";

//XXX probably needed by TempusDominus 5+
//                public final java.util.Date minDateAsJavaUtilDate() {
//                    return asJavaUtilDate(getMinDate());
//                }
//
//                public final java.util.Date maxDateAsJavaUtilDate() {
//                    return asJavaUtilDate(getMaxDate());
//                }
//
//                private static java.util.Date asJavaUtilDate(final String input) {
//                    return new Date(
//                            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(input, OffsetDateTime::from)
//                            .toEpochSecond());
//                }

                /**
                 * Whether the date picker should automatically be shown when the field gains focus.
                 *
                 * <p>
                 *     The default is to show the picker when the user clicks into the field.
                 * </p>
                 *
                 * <p>
                 *     Corresponds to the <code>allowInputToggle</code> config property of the underlying library,
                 *     (TempusDominus).
                 * </p>
                 */
                private boolean popupOnFocus = false;

            }

            private final DevelopmentUtilities developmentUtilities = new DevelopmentUtilities();
            @Data
            public static class DevelopmentUtilities {

                /**
                 * Determines whether debug bar and other stuff influenced by
                 * <code>DebugSettings#isDevelopmentUtilitiesEnabled()</code> is enabled or not.
                 *
                 * <p>
                 *     By default, depends on the mode (prototyping = enabled, server = disabled).  This property acts as an override.
                 * </p>
                 */
                private boolean enable = false;
            }

            private final RememberMe rememberMe = new RememberMe();
            @Data
            public static class RememberMe {
                /**
                 * Whether the sign-in page should have a &quot;remember me&quot; link (the default), or if it should
                 * be suppressed.
                 *
                 * <p>
                 *     If &quot;remember me&quot; is available and checked, then the viewer will allow users to login
                 *     based on encrypted credentials stored in a cookie.  An {@link #getEncryptionKey() encryption key}
                 *     can optionally be specified.
                 * </p>
                 */
                private boolean suppress = false;

                /**
                 * If the &quot;remember me&quot; feature is available, specifies the key to hold the encrypted
                 * credentials in the cookie.
                 */
                private String cookieKey = "causewayWicketRememberMe";
                /**
                 * If the &quot;remember me&quot; feature is available, optionally specifies an encryption key
                 * (a complex string acting as salt to the encryption algorithm) for computing the encrypted
                 * credentials.
                 *
                 * <p>
                 *     If not set, then (in production mode) the Wicket viewer will compute a random key each time it
                 *     is started.  This will mean that any credentials stored between sessions will become invalid.
                 * </p>
                 *
                 * <p>
                 *     Conversely, if set then (in production mode) then the same salt will be used each time the app
                 *     is started, meaning that cached credentials can continue to be used across restarts.
                 * </p>
                 *
                 * <p>
                 *     In prototype mode this setting is effectively ignored, because the same key will always be
                 *     provided (either as set, or a fixed literal otherwise).
                 * </p>
                 */
                private Optional<String> encryptionKey = Optional.empty();
            }

            private final Themes themes = new Themes();
            @Data
            public static class Themes {

                /**
                 * A comma separated list of enabled theme names, as defined by https://bootswatch.com.
                 */
                private List<String> enabled = listOf("Cosmo","Flatly","Darkly","Sandstone","United");

                /**
                 * The initial theme to use.
                 *
                 * <p>
                 *     Expected to be in the list of {@link #getEnabled()} themes.
                 * </p>
                 */
                @NotEmpty @NotNull
                private String initial = "Flatly";

                /**
                 * Whether the theme chooser widget should be available in the footer.
                 */
                private boolean showChooser = false;
            }

            private final Welcome welcome = new Welcome();
            @Data
            public static class Welcome {

                /**
                 * Text to be displayed on the application’s home page, used as a fallback if
                 * welcome.file is not specified. If a @HomePage action exists, then that will take
                 * precedence.
                 */
                private String text;
            }

            private final MessagePopups messagePopups = new MessagePopups();
            @Data
            public static class MessagePopups {

                /**
                 * How long the info popup should display before disappearing.
                 *
                 * <p>
                 *     A value of 0 means do not disappear automatically.
                 * </p>
                 */
                Duration infoDelay = Duration.ofMillis(3500);

                /**
                 * How long the warning popup should display before disappearing.
                 *
                 * <p>
                 *     A value of 0 (the default) means do not disappear automatically.
                 * </p>
                 */
                Duration warningDelay = Duration.ofMillis(0);

                /**
                 * How long the error popup should display before disappearing.
                 *
                 * <p>
                 *     A value of 0 (the default) means do not disappear automatically.
                 * </p>
                 */
                Duration errorDelay = Duration.ofMillis(0);

                /**
                 * How far in from the edge the popup should display
                 */
                int offset = 100;

                private final Placement placement = new Placement();
                @Data
                public static class Placement {

                    public static enum Vertical {
                        TOP, BOTTOM
                    }

                    public static enum Horizontal {
                        LEFT, RIGHT
                    }

                    /**
                     * Whether to display popups at the top or the bottom of the page.
                     *
                     * <p>
                     * The default is to show them at the top.
                     * </p>
                     */
                    Vertical vertical = Vertical.TOP;

                    /**
                     * Whether to display popups aligned ot the left or right of the page.
                     *
                     * <p>
                     * The default is to show them aligned to the right
                     * </p>
                     */
                    Horizontal horizontal = Horizontal.RIGHT;
                }
            }
        }
    }

    private final ValueTypes valueTypes = new ValueTypes();
    @Data
    public static class ValueTypes {

        private final Temporal temporal = new Temporal();
        @Data
        public static class Temporal {
            private final TemporalEditingPattern editing = new TemporalEditingPattern();
            private final TemporalDisplayPattern display = new TemporalDisplayPattern();
        }

        private final BigDecimal bigDecimal = new BigDecimal();
        @Data
        public static class BigDecimal {

            /**
             * Indicates how to derive the min fractional facet (the minimum number of digits after the decimal point).
             *
             * <p>
             * If this flag is set, then the {@link Digits#fraction()} annotation or ORM equivalent (the JDO
             * <code>@Column#scale</code> or the JPA {@link Column#scale()}) should be used for the
             * <code>MinFractionalFacet</code> as well as the <code>MaxFractionalFacet</code>.
             * </p>
             *
             * <p>
             * What this means in practice is that a numeric values will be rendered to the same number of fractional
             * digits, irrespective of whether they are whole numbers or fractional.  For example, with a scale of 2,
             * then &quot;123.4532&quot; will be rendered as &quot;123.45&quot;, while &quot;123&quot; will be rendered
             * as &quot;123.00&quot;.
             * </p>
             *
             * <p>
             * If this flag is NOT set, or if it is set but there is no annotation, then the {@link #minScale} config
             * property is used as a fallback.
             * </p>
             *
             * <p>
             * If there is no fallback, then it means that a big decimal such as &quot;123.00&quot; will be presented as
             * just &quot;123&quot; (that is, the shortest possible textual representation).
             * </p>
             */
            private boolean useScaleForMinFractionalFacet = true;

            /**
             * The minimum scale to use for all {@link java.math.BigDecimal}s.
             *
             * <p>
             * Is only used if the minimum scale has not been specified explicitly by some other means, typically
             * either {@link Digits#fraction()} or an ORM semantic such as the (JPA) {@link Column#scale()}.
             *
             * @deprecated - use {@link Display#getMinScale()} instead
             */
            @Deprecated(since = "2.2.0")
            private Integer minScale = null;

            @DeprecatedConfigurationProperty(replacement = "causeway.value-types.big-decimal.display.min-scale", reason = "Moved")
            public Integer getMinScale() {
                return minScale;
            }

            private final Editing editing = new Editing();
            @Data
            public static class Editing {
                /**
                 * A common use of {@link java.math.BigDecimal} is as a money value.  In some locales (eg English), the
                 * &quot;,&quot; (comma) is the grouping (thousands) separator wihle the &quot;.&quot; (period) acts as a
                 * decimal point, but in others (eg France, Italy) it is the other way around.
                 *
                 * <p>
                 *     Surprisingly perhaps, a string such as "123,99", when parsed ((by {@link java.text.DecimalFormat})
                 *     in an English locale, is not rejected but instead is evaluated as the value 12399L.  That's almost
                 *     certainly not what the end-user would have expected, and results in a money value 100x too large.
                 * </p>
                 *
                 * <p>
                 *     The purpose of this configuration property is to remove the confusion by simply disallowing the
                 *     thousands separator from being part of the input string.
                 * </p>
                 *
                 * <p>
                 *     For maximum safety, allowing the grouping separator is disallowed, but the alternate (original)
                 *     behaviour can be reinstated by setting this config property back to <code>true</code>.
                 * </p>
                 *
                 * <p>
                 *     The same configuration property is also used for rendering the value.
                 * </p>
                 *
                 * @see Display#isUseGroupingSeparator()
                 */
                private boolean useGroupingSeparator = false;
                /**
                 * When a BigDecimal is presented for editing, whether it should enforce the scale, possibly meaning
                 * trailing '0's to pad).  This is probably appropriate for BigDecimals that represent a money amount.
                 */
                private boolean preserveScale = false;
            }

            private final Display display = new Display();
            @Data
            public static class Display {

                /**
                 * The minimum scale to use for all {@link java.math.BigDecimal}s.
                 *
                 * <p>
                 * Is only used if the minimum scale has not been specified explicitly by some other means, typically
                 * either {@link Digits#fraction()} or an ORM semantic such as the (JPA) {@link Column#scale()}.
                 */
                private Integer minScale = null;

                /**
                 * Whether to use a grouping (thousands) separator (eg the &quot;,&quot; (comma) in the English locale)
                 * when rendering a big decimal.
                 *
                 * @see Editing#isUseGroupingSeparator()
                 */
                private boolean useGroupingSeparator = true;
            }

            /**
             * A common use of {@link java.math.BigDecimal} is as a money value.  In some locales (eg English), the
             * &quot;,&quot; (comma) is the grouping (thousands) separator wihle the &quot;.&quot; (period) acts as a
             * decimal point, but in others (eg France, Italy) it is the other way around.
             *
             * <p>
             *     Surprisingly perhaps, a string such as "123,99", when parsed ((by {@link java.text.DecimalFormat})
             *     in an English locale, is not rejected but instead is evaluated as the value 12399L.  That's almost
             *     certainly not what the end-user would have expected, and results in a money value 100x too large.
             * </p>
             *
             * <p>
             *     The purpose of this configuration property is to remove the confusion by simply disallowing the
             *     thousands separator from being part of the input string.
             * </p>
             *
             * <p>
             *     For maximum safety, allowing the grouping separator is disallowed, but the alternate (original)
             *     behaviour can be reinstated by setting this config property back to <code>true</code>.
             * </p>
             *
             * <p>
             *     The same configuration property is also used for rendering the value.
             * </p>
             *
             * @deprecated - use {@link Editing#isUseGroupingSeparator()} instead.
             */
            @Deprecated(since = "2.2.0")
            private boolean useGroupingSeparator = false;

            @DeprecatedConfigurationProperty(replacement = "causeway.value-types.big-decimal.editing.use-grouping-separator")
            public boolean isUseGroupingSeparator() {
                return useGroupingSeparator;
            }
        }

        private final Kroki kroki = new Kroki();
        @Data
        public static class Kroki {
            /**
             * If set, adds plantuml support to the AsciiDoc value type.
             * <p>
             * Eg. via docker instance like {@literal yuzutech/kroki}.
             */
            private URL backendUrl = null;

            /**
             * Max time for requests to the {@link #getBackendUrl()},
             * when waiting for a response. (default: 5 seconds)
             */
            private Duration requestTimeout = Duration.ofMillis(5000);
        }

    }

    private final Testing testing = new Testing();
    @Data
    public static class Testing {
        private final Fixtures fixtures = new Fixtures();
        @Data
        public static class Fixtures {
            /**
             * Indicates the fixture script class to run initially.
             *
             * <p>
             *     Intended for use when prototyping against an in-memory database (but will run in production mode
             *     as well if required).
             * </p>
             */
            @AssignableFrom("org.apache.causeway.testing.fixtures.applib.fixturescripts.FixtureScript")
            private Class<?> initialScript = null;

            private final FixtureScriptsSpecification fixtureScriptsSpecification = new FixtureScriptsSpecification();
            @Data
            public static class FixtureScriptsSpecification {
                /**
                 * Specifies the base package from which to search for fixture scripts.
                 *
                 * <p>
                 *     Either this or {@link #getPackagePrefix() packagePrefix} must be specified.  This property is
                 *     used by preference.
                 * </p>
                 *
                 * @see #getPackagePrefix()
                 */
                private Class<?> contextClass = null;
                /**
                 * Specifies the base package from which to search for fixture scripts.
                 *
                 * <p>
                 *     Either this or {@link #getContextClass()} must be specified; {@link #getContextClass()} is
                 *     used by preference.
                 * </p>
                 *
                 * @see #getContextClass()
                 */
                private String packagePrefix = null;

                /**
                 * How to handle objects that are to be added into a <code>FixtureResult</code> but which are not yet
                 * persisted.
                 */
                public enum NonPersistedObjectsStrategy {
                    PERSIST,
                    IGNORE
                }

                /**
                 * How to handle fixture scripts that are submitted to be executed more than once.
                 *
                 * <p>
                 *     Note that this is a global setting of the <code>FixtureScripts</code> service; there isn't
                 *     (currently) any way to mix-and-match fixture scripts that are written with differing semantics
                 *     in mind.  Ideally it should be the responsibility of the fixture script itself to determine
                 *     whether it should be run.
                 * </p>
                 */
                public enum MultipleExecutionStrategy {
                    /**
                     * Any given fixture script (or more precisely, any fixture script instance for a particular fixture script
                     * class) can only be run once.
                     *
                     * <p>
                     *     This strategy represents the original design of fixture scripts service.  Specifically, it allows an
                     *     arbitrary graph of fixture scripts (eg A -> B -> C, A -> B -> D, A -> C -> D) to be created each
                     *     specifying its dependencies, and without having to worry or co-ordinate whether those prerequisite
                     *     fixture scripts have already been run.
                     * </p>
                     * <p>
                     *     The most obvious example is a global teardown script; every fixture script can require this to be
                     *     called, but it will only be run once.
                     * </p>
                     * <p>
                     *     Note that this strategy treats fixture scripts as combining both the 'how' (which business action(s) to
                     *     call) and the also the 'what' (what the arguments are to those actions).
                     * </p>
                     */
                    EXECUTE_ONCE_BY_CLASS,
                    /**
                     * Any given fixture script can only be run once, where the check to determine if a fixture script has already
                     * been run is performed using value semantics.
                     *
                     * <p>
                     *     This strategy is a half-way house between the {@link #EXECUTE_ONCE_BY_VALUE} and {@link #EXECUTE}
                     *     strategies, where we want to prevent a fixture from running more than once, where by "fixture" we mean
                     *     the 'what' - the data to be loaded up; the 'how' is unimportant.
                     * </p>
                     *
                     * <p>
                     *     This strategy was introduced in order to better support the <tt>ExcelFixture</tt> fixture script
                     *     (provided by the (non-ASF) Causeway Addons'
                     *     <a href="https://github.com/causewayaddons/causeway-module-excel">Excel module</a>.  The <tt>ExcelFixture</tt>
                     *     takes an Excel spreadsheet as the 'what' and loads up each row.  So the 'how' is re-usable (therefore
                     *     the {@link #EXECUTE_ONCE_BY_CLASS} doesn't apply) on the other hand we don't want the 'what' to be
                     *     loaded more than once (so the {@link #EXECUTE} strategy doesn't apply either).  The solution is for
                     *     <tt>ExcelFixture</tt> to have value semantics (a digest of the spreadsheet argument).
                     * </p>
                     */
                    EXECUTE_ONCE_BY_VALUE,
                    /**
                     * Allow fixture scripts to run as requested.
                     *
                     * <p>
                     *     This strategy is conceptually the simplest; all fixtures are run as requested.  However, it is then
                     *     the responsibility of the programmer to ensure that fixtures do not interfere with each other.  For
                     *     example, if fixture A calls fixture B which calls teardown, and fixture A also calls fixture C that
                     *     itself calls teardown, then fixture B's setup will get removed.
                     * </p>
                     * <p>
                     *     The workaround to the teardown issue is of course to call the teardown fixture only once in the test
                     *     itself; however even then this strategy cannot cope with arbitrary graphs of fixtures.  The solution
                     *     is for the fixture list to be flat, one level high.
                     * </p>
                     */
                    EXECUTE;
                }

                /**
                 * Indicates whether, if a fixture script (or more precisely any other fixture scripts of the same
                 * class) is encountered more than once in a graph of dependencies, it should be executed again or
                 * skipped.
                 *
                 * <p>
                 *     The default is to fixture scripts are executed only once per class.
                 * </p>
                 *
                 * <p>
                 * Note that this policy can be overridden on a fixture-by-fixture basis if the fixture implements
                 * <code>FixtureScriptWithExecutionStrategy</code>.
                 * </p>
                 */
                private MultipleExecutionStrategy multipleExecutionStrategy = MultipleExecutionStrategy.EXECUTE_ONCE_BY_CLASS;

                /**
                 * Indicates whether objects that are returned as a fixture result should be automatically persisted
                 * if required (the default) or not.
                 */
                private NonPersistedObjectsStrategy nonPersistedObjectsStrategy = NonPersistedObjectsStrategy.PERSIST;

                @AssignableFrom("org.apache.causeway.testing.fixtures.applib.fixturescripts.FixtureScript")
                private Class<?> recreate = null;

                @AssignableFrom("org.apache.causeway.testing.fixtures.applib.fixturescripts.FixtureScript")
                private Class<?> runScriptDefault = null;

            }

        }
    }

    private final Extensions extensions = new Extensions();
    @Valid
    @Data
    public static class Extensions {

        private final AuditTrail auditTrail = new AuditTrail();
        @Data
        public static class AuditTrail {

            /**
             * As per {@link AuditTrail#getPersist()}.
             *
             * <p>
             *     Implementation note: we use an enum here (rather than a simple boolean) to allow for future
             *     enhancements.
             * </p>
             */
            public enum PersistPolicy {
                /**
                 * Persist to the audit trail.  This is the default.
                 */
                ENABLED,
                /**
                 * Do <i>NOT</i> persist to the audit trail.
                 */
                DISABLED;

                public boolean isEnabled() { return this == ENABLED; }
                public boolean isDisabled() { return this == DISABLED; }
            }

            /**
             * Whether the {@link EntityPropertyChangeSubscriber} implementation provided by this extension (which
             * persists property changes to the audit trail) is enabled or not.
             *
             * <p>
             *     One reason to use this option is if you wish to provide your own implementation that wraps
             *     or delegates to the default implementation of {@link EntityPropertyChangeSubscriber} that is
             *     provided by the <i>audittrail</i> extension.  Because entity property changes are published to
             *     <i>all</i> subscribers on the class path, you can disable the default implementation from
             *     doing anything using this setting.
             * </p>
             */
            private PersistPolicy persist = PersistPolicy.ENABLED;
        }

        private final CommandLog commandLog = new CommandLog();
        @Data
        public static class CommandLog {

            /**
             * As per {@link CommandLog#getPersist()}.
             *
             * <p>
             *     Implementation note: we use an enum here (rather than a simple boolean) to allow for future
             *     enhancements.
             * </p>
             */
            public enum PersistPolicy {
                /**
                 * Persist to the command log .  This is the default.
                 */
                ENABLED,
                /**
                 * Do <i>NOT</i> persist to the command log.
                 */
                DISABLED;

                public boolean isEnabled() { return this == ENABLED; }
                public boolean isDisabled() { return this == DISABLED; }
            }

            /**
             * Whether the {@link CommandSubscriber} implementation
             * provided by this extension (which persists commands to the command log) is enabled or not.
             *
             * <p>
             *     One reason to use this option is if you wish to provide your own implementation that wraps
             *     or delegates to the default implementation of {@link CommandSubscriber} that is
             *     provided by the <i>commandlog</i> extension.  Because commands are published to
             *     <i>all</i> subscribers on the class path, you can disable the default implementation from
             *     doing anything using this setting.
             * </p>
             */
            private PersistPolicy persist = PersistPolicy.ENABLED;

            private final RunBackgroundCommands runBackgroundCommands = new RunBackgroundCommands();
            @Data
            public static class RunBackgroundCommands {

                /**
                 * If the attempt to execute the command in the background results in failure,
                 * what should the processing do?
                 */
                public enum OnFailurePolicy {
                    /**
                     * If a failure has occurred, then capture the exception on the <code>CommandLogEntry</code> and
                     * then move onto the next command
                     *
                     * <p>
                     *     This policy allows further processing to continue, but runs the risk that the failed command
                     *     is not noticed by anyone, producing perhaps erroneous results later if it is run
                     *     out-of-order.
                     * </p>
                     */
                    CONTINUE_WITH_NEXT,
                    /**
                     * If a failure has occurred, then leave the <code>CommandLogEntry</code> unchanged, so that it
                     * is picked up again to be retried.
                     *
                     * <p>
                     *     This policy is in effect a 'stop the line' or 'fail fast' approach.  Because the background
                     *     command only runs in serial, an exception will result in no other commands being executed.
                     * </p>
                     */
                    STOP_THE_LINE;
                }

                /**
                 * Limits the number of pending commands that the <code>RunBackgroundCommandsJob</code>
                 * will execute.  After these have been executed, any <code>RunBackgroundCommandsJobListener</code>s are called.
                 *
                 * <p>
                 *     By default, quartz runs this command every 10 seconds, so the size should be proportion to that.
                 * </p>
                 */
                private int batchSize = 25;

                /**
                 * If there is an exception executing one of the commands, what should be done?
                 */
                private OnFailurePolicy onFailurePolicy = OnFailurePolicy.STOP_THE_LINE;
            }
        }

        private final CommandReplay commandReplay = new CommandReplay();
        @Data
        public static class CommandReplay {

            private final PrimaryAccess primaryAccess = new PrimaryAccess();
            @Data
            public static class PrimaryAccess {
                @jakarta.validation.constraints.Pattern(regexp="^http[s]?://[^:]+?(:\\d+)?.*([^/]+/)$")
                private Optional<String> baseUrlRestful = Optional.empty();
                private Optional<String> user = Optional.empty();
                private Optional<String> password = Optional.empty();
                @jakarta.validation.constraints.Pattern(regexp="^http[s]?://[^:]+?(:\\d+)?.*([^/]+/)$")
                private Optional<String> baseUrlWicket = Optional.empty();
            }

            private final SecondaryAccess secondaryAccess = new SecondaryAccess();
            @Data
            public static class SecondaryAccess {
                @jakarta.validation.constraints.Pattern(regexp="^http[s]?://[^:]+?(:\\d+)?.*([^/]+/)$")
                private Optional<String> baseUrlWicket = Optional.empty();
            }

            private Integer batchSize = 10;

            private final QuartzSession quartzSession = new QuartzSession();
            @Data
            public static class QuartzSession {
                /**
                 * The user that runs the replay session secondary.
                 */
                private String user = "causewayModuleExtCommandReplaySecondaryUser";
                private List<String> roles = listOf("causewayModuleExtCommandReplaySecondaryRole");
            }

            private final QuartzReplicateAndReplayJob quartzReplicateAndReplayJob = new QuartzReplicateAndReplayJob();
            @Data
            public static class QuartzReplicateAndReplayJob {
                /**
                 * Number of milliseconds before starting the job.
                 */
                private long startDelay = 15000;
                /**
                 * Number of milliseconds before running again.
                 */
                private long repeatInterval = 10000;
            }

            private final Analyser analyser = new Analyser();
            @Data
            public static class Analyser {
                private final Result result = new Result();
                @Data
                public static class Result {
                    private boolean enabled = true;
                }
                private final Exception exception = new Exception();
                @Data
                public static class Exception {
                    private boolean enabled = true;
                }

            }
        }

        private final Cors cors = new Cors();
        @Data
        public static class Cors {

            /**
             * Whether the resource supports user credentials.
             *
             * <p>
             *     This flag is exposed as part of 'Access-Control-Allow-Credentials' header in a pre-flight response.
             *     It helps browser determine whether or not an actual request can be made using credentials.
             * </p>
             *
             * <p>
             *     By default this is not set (i.e. user credentials are not supported).
             * </p>
             *
             * <p>
             *     For more information, check the usage of the <code>cors.support.credentials</code> init parameter
             *     for <a href="https://github.com/eBay/cors-filter">EBay CORSFilter</a>.
             * </p>
             */
            private boolean allowCredentials = false;

            /**
             * Which origins are allowed to make CORS requests.
             *
             * <p>
             *     The default is the wildcard (&quot;*&quot;), meaning any origin is allowed to access the resource,
             *     but this can be made more restrictive if necessary using a whitelist of comma separated origins
             *     eg:
             * </p>
             * <p>
             *     <code>http://www.w3.org, https://www.apache.org</code>
             * </p>
             *
             * <p>
             *     For more information, check the usage of the <code>cors.allowed.origins</code> init parameter
             *     for <a href="https://github.com/eBay/cors-filter">EBay CORSFilter</a>.
             * </p>
             */
            private List<String> allowedOrigins = listOf("*");

            /**
             * Which HTTP headers can be allowed in a CORS request.
             *
             * <p>
             *     These header will also be returned as part of 'Access-Control-Allow-Headers' header in a
             *     pre-flight response.
             * </p>
             *
             * <p>
             *     For more information, check the usage of the <code>cors.allowed.headers</code> init parameter
             *     for <a href="https://github.com/eBay/cors-filter">EBay CORSFilter</a>.
             * </p>
             */
            private List<String> allowedHeaders = listOf(
                    "Content-Type",
                    "Accept",
                    "Origin",
                    "Access-Control-Request-Method",
                    "Access-Control-Request-Headers",
                    "Authorization",
                    "Cache-Control",
                    "If-Modified-Since",
                    "Pragma");

            /**
             * Which HTTP methods are permitted in a CORS request.
             *
             * <p>
             *     A comma separated list of HTTP methods that can be used to access the resource, using cross-origin
             *     requests. These are the methods which will also be included as part of 'Access-Control-Allow-Methods'
             *     header in a pre-flight response.
             * </p>
             *
             * <p>
             *     Default is <code>GET</code>, <code>POST</code>, <code>HEAD</code>, <code>OPTIONS</code>.
             * </p>
             *
             * <p>
             *     For more information, check the usage of the <code>cors.allowed.methods</code> init parameter
             *     for <a href="https://github.com/eBay/cors-filter">EBay CORSFilter</a>.
             * </p>
             */
            private List<String> allowedMethods = listOf("GET","PUT","DELETE","POST","OPTIONS");

            /**
             * Which HTTP headers are exposed in a CORS request.
             *
             * <p>
             *     A comma separated list of headers other than the simple response headers that browsers are allowed
             *     to access. These are the headers which will also be included as part of
             *     'Access-Control-Expose-Headers' header in the pre-flight response.
             * </p>
             *
             * <p>
             *     Default is none.
             * </p>
             *
             * <p>
             *     For more information, check the usage of the <code>cors.exposed.headers</code> init parameter
             *     for <a href="https://github.com/eBay/cors-filter">EBay CORSFilter</a>.
             * </p>
             */
            private List<String> exposedHeaders = listOf("Authorization");

        }

        private final ExecutionLog executionLog = new ExecutionLog();
        @Data
        public static class ExecutionLog {

            /**
             * As per {@link ExecutionLog#getPersist()}.
             *
             * <p>
             *     Implementation note: we use an enum here (rather than a simple boolean) to allow for future
             *     enhancements.
             * </p>
             */
            public enum PersistPolicy {
                /**
                 * Persist to the execution log.  This is the default.
                 */
                ENABLED,
                /**
                 * Do <i>NOT</i> persist to the execution log.
                 */
                DISABLED;

                public boolean isEnabled() { return this == ENABLED; }
                public boolean isDisabled() { return this == DISABLED; }
            }

            /**
             * Whether the {@link ExecutionSubscriber} implementation
             * provided by this extension (which persists executions to the execution log) is enabled or not.
             *
             * <p>
             *     One reason to use this option is if you wish to provide your own implementation that wraps
             *     or delegates to the default implementation of {@link ExecutionSubscriber} that is
             *     provided by the <i>executionLog</i> extension.  Because executions are published to
             *     <i>all</i> subscribers on the class path, you can disable the default implementation from
             *     doing anything using this setting.
             * </p>
             */
            private PersistPolicy persist = PersistPolicy.ENABLED;
        }

        private final ExecutionOutbox executionOutbox = new ExecutionOutbox();
        @Valid
        @Data
        public static class ExecutionOutbox {

            private final RestApi restApi = new RestApi();
            @Valid
            @Data
            public static class RestApi {
                /**
                 * The maximum number of interactions that will be returned when the REST API is polled.
                 */
                @Min(value = 1)
                @Max(value = 1000)
                private int maxPending = 100;
            }

            /**
             * As per {@link ExecutionLog#getPersist()}.
             *
             * <p>
             *     Implementation note: we use an enum here (rather than a simple boolean) to allow for future
             *     enhancements.
             * </p>
             */
            public enum PersistPolicy {
                /**
                 * Persist to the outbox.  This is the default.
                 */
                ENABLED,
                /**
                 * Do <i>NOT</i> persist to the outbox.
                 */
                DISABLED;

                public boolean isEnabled() { return this == ENABLED; }
                public boolean isDisabled() { return this == DISABLED; }
            }

            /**
             * Whether the {@link ExecutionSubscriber}
             * implementation provided by this extension (which persists executions to the outbox) is enabled or not.
             *
             * <p>
             *     One reason to use this option is if you wish to provide your own implementation that wraps
             *     or delegates to the outbox implementation of {@link ExecutionSubscriber} that is
             *     provided by the <i>executionOutbox</i> extension.  Because executions are published to
             *     <i>all</i> subscribers on the class path, you can disable the outbox implementation from
             *     doing anything using this setting.
             * </p>
             */
            private ExecutionOutbox.PersistPolicy persist = ExecutionOutbox.PersistPolicy.ENABLED;

        }

        private final LayoutLoaders layoutLoaders = new LayoutLoaders();
        @Data
        public static class LayoutLoaders {

            private final Github github = new Github();
            @Data
            public static class Github {

                /**
                 * eg <code>apache/causeway-app-simpleapp</code>
                 */
                private String repository;

                /**
                 * As per <a href="https://github.com/settings/tokens">https://github.com/settings/tokens</a>,
                 * must have permissions to the <code>/search</code> and <code>/contents</code> APIs for the specified
                 * {@link #getRepository() repository}.
                 */
                private String apiKey;
            }
        }

        private final Secman secman = new Secman();
        @Data
        public static class Secman {

            private final Seed seed = new Seed();
            @Data
            public static class Seed {

                public static final String ADMIN_USER_NAME_DEFAULT = "secman-admin";
                public static final String ADMIN_PASSWORD_DEFAULT = "pass";
                public static final String ADMIN_ROLE_NAME_DEFAULT = "causeway-ext-secman-admin";
                public static final List<String> ADMIN_STICKY_NAMESPACE_PERMISSIONS_DEFAULT =
                        Collections.unmodifiableList(listOf(
                                CausewayModuleApplib.NAMESPACE,
                                CausewayModuleApplib.NAMESPACE_SUDO,
                                CausewayModuleApplib.NAMESPACE_CONF,
                                CausewayModuleApplib.NAMESPACE_FEAT,
                                "causeway.security",
                                "causeway.ext.h2Console",
                                "causeway.ext.secman",
                                "causeway.ext.layoutLoaders"
                        ));
                public static final List<String> ADMIN_ADDITIONAL_NAMESPACE_PERMISSIONS =
                        Collections.unmodifiableList(listOf());
                public static final String REGULAR_USER_ROLE_NAME_DEFAULT = "causeway-ext-secman-user";
                public static final boolean AUTO_UNLOCK_IF_DELEGATED_AND_AUTHENTICATED_DEFAULT = false;

                /**
                 * Path to local YAML file, if present, to use as an alternative seeding strategy.
                 * <p>
                 * Eg. seed from a YAML file, that was previously exported by SecMan's
                 * ApplicationRoleManager_exportAsYaml mixin.
                 */
                private String yamlFile = null;

                private final Admin admin = new Admin();
                @Data
                public static class Admin {

                    /**
                     * The name of the security super user.
                     *
                     * <p>
                     * This user is automatically made a member of the
                     * {@link #getRoleName() admin role}, from which it is granted
                     * permissions to administer other users.
                     * </p>
                     *
                     * <p>
                     * The password for this user is set in {@link Admin#getPassword()}.
                     * </p>
                     *
                     * @see #getPassword()
                     * @see #getRoleName()
                     */
                    private String userName = ADMIN_USER_NAME_DEFAULT;

                    // sonar-ignore-on (detects potential security risk, which we are aware of)
                    /**
                     * The corresponding password for {@link #getUserName() admin user}.
                     *
                     * @see #getUserName()
                     */
                    private String password = ADMIN_PASSWORD_DEFAULT;
                    // sonar-ignore-off

                    /**
                     * The name of security admin role.
                     *
                     * <p>
                     * Users with this role (in particular, the default
                     * {@link Admin#getUserName() admin user} are granted access to a set of
                     * namespaces ({@link NamespacePermissions#getSticky()} and
                     * {@link NamespacePermissions#getAdditional()}) which are intended to
                     * be sufficient to allow users with this admin role to be able to
                     * administer the security module itself, for example to manage users and
                     * roles.
                     * </p>
                     *
                     * @see Admin#getUserName()
                     * @see NamespacePermissions#getSticky()
                     * @see NamespacePermissions#getAdditional()
                     */
                    private String roleName = ADMIN_ROLE_NAME_DEFAULT;

                    private final NamespacePermissions namespacePermissions = new NamespacePermissions();
                    @Data
                    public static class NamespacePermissions {

                        /**
                         * The set of namespaces to which the {@link Admin#getRoleName() admin role}
                         * is granted.
                         *
                         * <p>
                         * These namespaces are intended to be sufficient to allow users with
                         * this admin role to be able to administer the security module itself,
                         * for example to manage users and roles.  The security user is not
                         * necessarily able to use the main business logic within the domain
                         * application itself, though.
                         * </p>
                         *
                         * <p>
                         * These roles cannot be removed via user interface
                         * </p>
                         *
                         * <p>
                         * WARNING: normally these should not be overridden.  Instead, specify
                         * additional namespaces using
                         * {@link NamespacePermissions#getAdditional()}.
                         * </p>
                         *
                         * @see NamespacePermissions#getAdditional()
                         */
                        private List<String> sticky = ADMIN_STICKY_NAMESPACE_PERMISSIONS_DEFAULT;

                        /**
                         * An (optional) additional set of namespaces that the
                         * {@link Admin#getRoleName() admin role} is granted.
                         *
                         * <p>
                         * These are in addition to the main
                         * {@link NamespacePermissions#getSticky() namespaces} granted.
                         * </p>
                         *
                         * @see NamespacePermissions#getSticky()
                         */
                        private List<String> additional = ADMIN_ADDITIONAL_NAMESPACE_PERMISSIONS;

                    }

                }

                private final RegularUser regularUser = new RegularUser();
                @Data
                public static class RegularUser {

                    /**
                     * The role name for regular users of the application, granting them access
                     * to basic security features.
                     *
                     * <p>
                     *     The exact set of permissions is hard-wired in the
                     *     <code>CausewayExtSecmanRegularUserRoleAndPermissions</code> fixture.
                     * </p>
                     */
                    private String roleName = REGULAR_USER_ROLE_NAME_DEFAULT;

                }

            }

            private final FixtureScripts fixtureScripts = new FixtureScripts();
            @Data
            public static class FixtureScripts {

                private final AbstractRoleAndPermissionsFixtureScript abstractRoleAndPermissionsFixtureScript = new AbstractRoleAndPermissionsFixtureScript();
                @Data
                public static class AbstractRoleAndPermissionsFixtureScript {

                    public enum UnknownFeatureIdCheckingPolicy {
                        /**
                         * Do not check whether the featureIds passed in actually exist.
                         */
                        IGNORE,
                        /**
                         * Check that the featureIds passed in actually exist, and fail immediately if not.
                         */
                        FAIL_FAST,
                        ;

                        public boolean isIgnore() { return this == IGNORE; }
                        public boolean isFailFast() { return this == FAIL_FAST; }
                    }

                    /**
                     * Whether to check if every featureId passed in exists or not.
                     */
                    private UnknownFeatureIdCheckingPolicy unknownFeatureIdCheckingPolicy = UnknownFeatureIdCheckingPolicy.IGNORE;
                }

            }

            private final DelegatedUsers delegatedUsers = new DelegatedUsers();

            @Data
            public static class DelegatedUsers {

                public enum AutoCreatePolicy {
                    AUTO_CREATE_AS_LOCKED,
                    AUTO_CREATE_AS_UNLOCKED,
                    DO_NOT_AUTO_CREATE,
                }

                /**
                 * Whether delegated users should be autocreated as locked (the default) or unlocked.
                 *
                 * <p>
                 * BE AWARE THAT if any users are auto-created as unlocked, then the set of roles that
                 * they are given should be highly restricted !!!
                 * </p>
                 *
                 * <p>
                 *     NOTE also that this configuration policy is ignored if running secman with Spring OAuth2
                 *     or Keycloak as the authenticator; users are always auto-created.
                 * </p>
                 */
                private AutoCreatePolicy autoCreatePolicy = AutoCreatePolicy.AUTO_CREATE_AS_LOCKED;

                /**
                 * The set of roles that users that have been automatically created are granted automatically.
                 *
                 * <p>
                 *     Typically the regular user role (as per <code>causeway.secman.seed.regular-user.role-name</code>,
                 *     default value of <code>causeway-ext-secman-user</code>) will be one of the roles listed here, to
                 *     provide the ability for the end-user to logout, among other things (!).
                 * </p>
                 */
                private List<String> initialRoleNames = new ArrayList<>();

            }

            public enum PermissionsEvaluationPolicy {
                ALLOW_BEATS_VETO,
                VETO_BEATS_ALLOW
            }

            /**
             * If there are conflicting (allow vs veto) permissions at the same scope, then this policy determines
             * whether to prefer to allow the permission or to veto it.
             *
             * <p>
             *     This is only used an implementation of secman's <code>PermissionsEvaluationService</code> SPI has
             *     not been provided explicitly.
             * </p>
             */
            private PermissionsEvaluationPolicy permissionsEvaluationPolicy = PermissionsEvaluationPolicy.ALLOW_BEATS_VETO;

            private final UserRegistration userRegistration = new UserRegistration();
            @Data
            public static class UserRegistration {
                /**
                 * The set of roles that users registering with the app are granted
                 * automatically.
                 *
                 * <p>
                 *     If using the wicket viewer, also requires
                 *     {@link Viewer.Wicket#isSuppressSignUp() causeway.viewer.wicket.suppress-signup} to be set
                 *     <code>false</code>, along with any other of its other prereqs.
                 * </p>
                 */
                private final List<String> initialRoleNames = new ArrayList<>();
            }

            public enum UserMenuMeActionPolicy {
                HIDE,
                DISABLE,
                ENABLE
            }

            /**
             * Whether the presence of SecMan should result in the automatic suppression of the
             * {@link org.apache.causeway.applib.services.userui.UserMenu}'s {@link UserMenu.me#act() me()} action.
             *
             * <p>
             *     This is normally what is required as SecMan's <code>ApplicationUser</code> is a more comprehensive
             *     representation of the current user.  If the default {@link UserMenu.me#act() me()} action is not
             *     suppressed, then the end-user will see two actions with the name &quot;me&quot; in the tertiary menu.
             * </p>
             */
            private UserMenuMeActionPolicy userMenuMeActionPolicy = UserMenuMeActionPolicy.HIDE;
        }

        private final SessionLog sessionLog = new SessionLog();
        @Data
        public static class SessionLog {
            boolean autoLogoutOnRestart = true;
        }

        private final Titlecache titlecache = new Titlecache();
        @Data
        public static class Titlecache {

            private final Caffeine caffeine = new Caffeine();
            @Data
            public static class Caffeine {
                /**
                 * Default duration that entries remain in the cache (for a given logical type name),
                 * in minutes.
                 *
                 * <p>
                 *     Default is 20 mins.
                 * </p>
                 */
                private int expiryDurationInMinutes = 20;

                /**
                 * Default maximum number of entries in the cache (for a given logical type name)
                 *
                 * <p>
                 *     Default is 1000
                 * </p>
                 */
                private int maxSizeInEntries = 1000;
            }
        }
    }

    private static List<String> listOf(final String ...values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    record PatternToString(
            Pattern pattern,
            String string) {
    }
    private static Map<Pattern, String> asMap(final String... mappings) {
        return new LinkedHashMap<>(_NullSafe.stream(mappings).map(mapping -> {
                    final String[] parts = mapping.split(":");
                    if (parts.length != 2) return null;
                    
                    try {
                        return new PatternToString(Pattern.compile(parts[0], Pattern.CASE_INSENSITIVE), parts[1]);
                    } catch(Exception ex) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toMap(PatternToString::pattern, PatternToString::string)));
    }

    @Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AssignableFromValidator.class)
    @Documented
    public @interface AssignableFrom {

        String value();

        String message()
                default "{org.apache.causeway.core.config.CausewayConfiguration.AssignableFrom.message}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }

    public static class AssignableFromValidator implements ConstraintValidator<AssignableFrom, Class<?>> {

        private Class<?> superType;

        @Override
        public void initialize(final AssignableFrom assignableFrom) {
            var className = assignableFrom.value();
            try {
                superType = _Context.loadClass(className);
            } catch (ClassNotFoundException e) {
                superType = null;
            }
        }

        @Override
        public boolean isValid(
                final Class<?> candidateClass,
                final ConstraintValidatorContext constraintContext) {
            if (superType == null || candidateClass == null) {
                return true;
            }
            return superType.isAssignableFrom(candidateClass);
        }
    }

    @Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = OneOfValidator.class)
    @Documented
    public @interface OneOf {

        String[] value();

        String message()
                default "{org.apache.causeway.core.config.CausewayConfiguration.OneOf.message}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }

    public static class OneOfValidator implements ConstraintValidator<OneOf, String> {

        private List<String> allowed;

        @Override
        public void initialize(final OneOf assignableFrom) {
            var value = assignableFrom.value();
            allowed = value != null? Collections.unmodifiableList(Arrays.asList(value)): Collections.emptyList();
        }

        @Override
        public boolean isValid(
                final String candidateValue,
                final ConstraintValidatorContext constraintContext) {
            return allowed.contains(candidateValue);
        }
    }

}
