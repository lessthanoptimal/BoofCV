/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.abst.feature.associate;

import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;


/**
 * Generalized interface for associating features.
 *
 * @author Peter Abeles
 */
public interface GeneralAssociation<T> {

	/**
	 * Finds the best match for each item in the src list with an item in the 'dst' list.
	 *
	 * @param listSrc Source list that is being matched to dst list.
	 * @param listDst Destination list of items that are matched to source.
	 */
	public void associate( FastQueue<T> listSrc , FastQueue<T> listDst );

	/**
	 * List of associated features.  Indexes refer to the index inside the input lists.
	 *
	 * @return List of associated features.
	 */
	public FastQueue<AssociatedIndex> getMatches();
}
