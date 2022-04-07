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
package org.apache.isis.core.metamodel.util;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

import org.apache.isis.applib.annotation.LabelPosition;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.layout.grid.bootstrap.BSGrid;
import org.apache.isis.commons.collections.Can;
import org.apache.isis.commons.internal.base._Casts;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.all.hide.HiddenFacet;
import org.apache.isis.core.metamodel.facets.collections.collection.defaultview.DefaultViewFacet;
import org.apache.isis.core.metamodel.facets.members.cssclass.CssClassFacet;
import org.apache.isis.core.metamodel.facets.object.autocomplete.AutoCompleteFacet;
import org.apache.isis.core.metamodel.facets.object.grid.GridFacet;
import org.apache.isis.core.metamodel.facets.object.icon.IconFacet;
import org.apache.isis.core.metamodel.facets.object.value.ValueFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.daterenderedadjust.DateRenderAdjustFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.labelat.LabelAtFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.maxlen.MaxLengthFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.multiline.MultiLineFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.typicallen.TypicalLengthFacet;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeature;

import lombok.val;
import lombok.experimental.UtilityClass;

/**
 * Facet utility.
 * <p>
 * Motivation: Viewers should not use {@link Facet}s directly.
 */
@UtilityClass
public final class Facets {

    public Can<ManagedObject> autoCompleteExecute(
            final ObjectSpecification objectSpec, final String term) {
        return objectSpec.lookupFacet(AutoCompleteFacet.class)
        .map(autoCompleteFacet->autoCompleteFacet.execute(term, InteractionInitiatedBy.USER))
        .orElseGet(Can::empty);
    }

    public boolean autoCompleteIsPresent(final ObjectSpecification objectSpec) {
        return objectSpec.containsFacet(AutoCompleteFacet.class);
    }

    public OptionalInt autoCompleteMinLength(final ObjectSpecification objectSpec) {
        return objectSpec.lookupFacet(AutoCompleteFacet.class)
        .map(AutoCompleteFacet::getMinLength)
        .map(OptionalInt::of)
        .orElseGet(OptionalInt::empty);
    }

    public Optional<BSGrid> bootstrapGrid(
            final ObjectSpecification objectSpec, final ManagedObject objectAdapter) {
        return objectSpec.lookupFacet(GridFacet.class)
        .map(gridFacet->gridFacet.getGrid(objectAdapter))
        .flatMap(grid->_Casts.castTo(BSGrid.class, grid));
    }

    public Optional<String> cssClassFor(
            final FacetHolder objectSpec, final ManagedObject objectAdapter) {
        return objectSpec.lookupFacet(CssClassFacet.class)
        .map(cssClassFacet->cssClassFacet.cssClass(objectAdapter));
    }

    public int dateRenderAdjustDays(final ObjectFeature feature) {
        return feature.lookupFacet(DateRenderAdjustFacet.class)
        .map(DateRenderAdjustFacet::getDateRenderAdjustDays)
        .orElse(0);
    }

    public boolean defaultViewIsPresent(final ObjectFeature feature) {
        return feature.containsFacet(DefaultViewFacet.class);
    }

    public boolean defaultViewIsTable(final ObjectFeature feature) {
        return defaultViewName(feature)
        .map(viewName->"table".equals(viewName))
        .orElse(false);
    }

    public Optional<String> defaultViewName(final ObjectFeature feature) {
        return feature.lookupFacet(DefaultViewFacet.class)
        .map(DefaultViewFacet::value);
    }

    public void gridPreload(
            final ObjectSpecification objectSpec, final ManagedObject objectAdapter) {
        objectSpec.lookupFacet(GridFacet.class)
        .ifPresent(gridFacet->
            // the facet should always exist, in fact
            // just enough to ask for the metadata.
            // This will cause the current ObjectSpec to be updated as a side effect.
            gridFacet.getGrid(objectAdapter));
    }

    public Optional<Where> hiddenWhere(final ObjectFeature feature) {
        return feature.lookupFacet(HiddenFacet.class)
        .map(HiddenFacet::where);
    }

    public Predicate<ObjectFeature> hiddenWhereMatches(final Predicate<Where> matcher) {
        return feature->Facets.hiddenWhere(feature)
        .map(matcher::test)
        .orElse(false);
    }

    public boolean iconIsPresent(final ObjectSpecification objectSpec) {
        return objectSpec.containsFacet(IconFacet.class);
    }

    public Optional<LabelPosition> labelAt(final ObjectFeature feature) {
        return feature.lookupFacet(LabelAtFacet.class)
        .map(LabelAtFacet::label);
    }

    public String labelAtCss(final ObjectFeature feature) {
        return Facets.labelAt(feature)
        .map(labelPos->{
            switch (labelPos) {
            case LEFT:
                return "label-left";
            case RIGHT:
                return "label-right";
            case NONE:
                return "label-none";
            case TOP:
                return "label-top";
            case DEFAULT:
            case NOT_SPECIFIED:
            default:
                return "label-left";
            }
        })
        .orElse("label-left");
    }

    public OptionalInt maxLength(final ObjectSpecification objectSpec) {
        return objectSpec
                .lookupFacet(MaxLengthFacet.class)
                .map(MaxLengthFacet::value)
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty);
    }

    public boolean multilineIsPresent(final ObjectFeature feature) {
        return feature.lookupNonFallbackFacet(MultiLineFacet.class)
                .isPresent();
    }

    public OptionalInt multilineNumberOfLines(final ObjectSpecification objectSpec) {
        return objectSpec
                .lookupFacet(MultiLineFacet.class)
                .map(MultiLineFacet::numberOfLines)
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty);
    }

    public OptionalInt typicalLength(
            final ObjectSpecification objectSpec, final OptionalInt maxLength) {
        val typicalLength = objectSpec
                .lookupFacet(TypicalLengthFacet.class)
                .map(TypicalLengthFacet::value)
                .orElse(null);
        // doesn't make sense for typical length to be > maxLength
        final Integer result = (typicalLength != null
                && maxLength.isPresent()
                && typicalLength > maxLength.getAsInt())
                ? maxLength.getAsInt()
                : typicalLength;
        return Optional.ofNullable(result)
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty);
    }

    public boolean valueIsPresent(final ObjectSpecification objectSpec) {
        return objectSpec.containsFacet(ValueFacet.class);
    }


}
