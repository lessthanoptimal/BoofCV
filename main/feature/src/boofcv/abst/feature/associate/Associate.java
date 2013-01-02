/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.associate;

import boofcv.struct.FastQueue;
import boofcv.struct.GrowingArrayInt;
import boofcv.struct.feature.AssociatedIndex;

/**
 * Common functions for associating features between two images with a single match. Features are associated from the
 * source image to the destination image.  Each source feature is paired up with a single feature in the destination.
 * If a match is not found then it is added to the unassociated list.
 *
 * @author Peter Abeles
 */
public interface Associate {

	/**
	 * Finds the best match for each item in the source list with an item in the destination list.
	 */
	public void associate();

	/**
	 * List of associated features.  Indexes refer to the index inside the input lists.
	 *
	 * @return List of associated features.
	 */
	public FastQueue<AssociatedIndex> getMatches();

	/**
	 * Indexes of features in the source set which are not associated to features to the destination set.
	 *
	 * @return List of unassociated source features by index.
	 */
	public GrowingArrayInt getUnassociatedSource();
}
