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
package org.apache.causeway.viewer.wicket.ui.components.collection.present.ajaxtable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;

import org.jspecify.annotations.Nullable;

import org.apache.causeway.applib.annotation.TableDecorator;
import org.apache.causeway.core.metamodel.facets.object.tabledec.TableDecoratorFacet;
import org.apache.causeway.core.metamodel.object.MmSortUtils;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;
import org.apache.causeway.viewer.wicket.model.models.coll.CollectionModel;
import org.apache.causeway.viewer.wicket.model.models.coll.DataRowWkt;

import org.jspecify.annotations.NonNull;

/**
 * Part of the {@link AjaxFallbackDefaultDataTable} API.
 */
public class CollectionContentsSortableDataProvider
extends SortableDataProvider<DataRow, String> {

    private static final long serialVersionUID = 1L;

    private final CollectionModel collectionModel;

    public CollectionContentsSortableDataProvider(final CollectionModel collectionModel) {
        this.collectionModel = collectionModel;
    }

    public boolean isDecoratedWithDataTablesNet() {
        return getDataTableModel().getMetaModel().getFacetHolder().lookupFacet(TableDecoratorFacet.class)
        .map(TableDecoratorFacet::value)
        .map(TableDecorator.DatatablesNet.class::equals)
        .orElse(false);
    }

    public DataTableInteractive getDataTableModel() {
        return applyColumnSortTo(dataTableInternal());
    }

    @Override
    public IModel<DataRow> model(final DataRow dataRow) {
        return new DataRowWkt(dataRow.rowIndex(), collectionModel);
    }

    @Override
    public long size() {
        return getDataTableModel().getFilteredElementCount();
    }

    @Override
    public Iterator<DataRow> iterator(final long skip, final long limit) {
        var dataTable = getDataTableModel();
        return dataTable.dataRowsFilteredAndSortedObservable().getValue()
                .iterator(Math.toIntExact(skip), Math.toIntExact(limit));
    }

    // -- HELPER

    private DataTableInteractive dataTableInternal() {
        return collectionModel.getObject();
    }

    private DataTableInteractive applyColumnSortTo(final DataTableInteractive dataTableInteractive) {
        // honor (single) column sort (if any)
        // optimization: set only, if value actually changes
        var oldColumnSort = dataTableInteractive.columnSortBindable().getValue();
        var newColumnSort = columnSort().orElse(null);
        if(!Objects.equals(oldColumnSort, newColumnSort)) {
            dataTableInteractive.columnSortBindable().setValue(newColumnSort);
        }
        return dataTableInteractive;
    }

    private Optional<DataTableInteractive.ColumnSort> columnSort() {
        var sortParam = getSort();
        return lookupColumnIndexFor(sortParam).stream()
                .mapToObj(columnIndex->
                    new DataTableInteractive.ColumnSort(columnIndex, sortDirection(sortParam)))
                .findFirst();
    }

    private OptionalInt lookupColumnIndexFor(final @Nullable SortParam<String> sortParam) {
        if(sortParam==null) return OptionalInt.empty();
        int columnIndex = 0;
        for(var column : dataTableInternal().dataColumnsObservable().getValue()) {
            if(column.associationMetaModel().getId().equals(sortParam.getProperty())) {
                return OptionalInt.of(columnIndex);
            }
            ++columnIndex;
        }
        return OptionalInt.empty();
    }

    private static MmSortUtils.SortDirection sortDirection(final @NonNull SortParam<String> sortParam) {
        return sortParam.isAscending()
                ? MmSortUtils.SortDirection.ASCENDING
                : MmSortUtils.SortDirection.DESCENDING;
    }

}