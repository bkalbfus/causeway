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
package org.apache.causeway.viewer.wicket.ui.components.tree;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.model.IModel;

import org.apache.causeway.applib.graph.tree.TreePath;
import org.apache.causeway.applib.graph.tree.TreeState;
import org.apache.causeway.core.metamodel.tree.TreeNodeMemento;

/**
 * Wicket's model for collapse/expand state
 */
record TreeExpansionModel(
    TreeState treeState,
    Set<TreeNodeMemento> expandedNodes) implements IModel<Set<TreeNodeMemento>> {

    public static TreeExpansionModel of(
            final TreeState treeState) {
        return new TreeExpansionModel(treeState);
    }

    private TreeExpansionModel(
            final TreeState treeState) {
        this(treeState, treeState.expandedNodePaths().stream()
                .map(TreeNodeMemento::new)
                .collect(Collectors.toSet()));
    }

    /**
     * Happens on user interaction via UI.
     * @param t
     */
    public void onExpand(final TreeNodeMemento t) {
        treeState.expandedNodePaths().add(t.treePath());
    }

    /**
     * Happens on user interaction via UI.
     * @param t
     */
    public void onCollapse(final TreeNodeMemento t) {
        treeState.expandedNodePaths().remove(t.treePath());
    }

    public boolean contains(final TreePath treePath) {
        return treeState.expandedNodePaths().contains(treePath);
    }

    public boolean isSelected(final TreePath treePath){
        return treeState.selectedNodePaths().contains(treePath);
    }

    @Override
    public Set<TreeNodeMemento> getObject() {
        return expandedNodes;
    }

    @Override
    public String toString() {
        return treeState.toString();
    }

}