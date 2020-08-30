/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.sfm.structure2;

import lombok.Getter;

/**
 * Selects a subset of views from a {@link SceneWorkingGraph} as the first step before performing local bundle
 * adjustment. The goal is to select the subset of views which would contribute the most to the target view's
 * state estimate. To keep computational limits in check the user needs to specifies a maximum number of views.
 *
 * Every connection between views is assigned a score. The goal is to find the subset of views which maximizes
 * the minimum score across all connections between views. This is approximated using the greedy algorithm below:
 *
 * Summary:
 * <ol>
 *     <li>Set of views will be the target's neighbors and their neighbors</li>
 *     <li>Create a list of all edges and their scores that connect these</li>
 *     <li>Repeat the loop below until only N views remain</li>
 *     <ol>
 *     <li>Select the edge with the lowest score</li>
 *     <li>Pick one of the views it is connected to be prune based on their connections</li>
 *     <li>Remove the view and all the edges connected to it</li>
 *     </ol>
 * </ol>
 *
 * @author Peter Abeles
 */
public class SelectNeighborsAroundView {

	/** Copy of the local scene which can be independently optimized */
	@Getter final SceneWorkingGraph localWorking = new SceneWorkingGraph();

	public void process(SceneWorkingGraph.View target , SceneWorkingGraph working) {
		// TODO create list of views
	}
}
