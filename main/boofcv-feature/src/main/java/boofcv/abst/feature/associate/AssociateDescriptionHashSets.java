/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Feature set aware association algorithm for use when there is a large sparse set of unique set ID's. Internally
 * a HashMap is used to look up the set ID's to the actual set. Internally it has an array 'sets' which stores
 * the actual data for each set. The set index does not correspond to the set ID, you need to use the map to
 * go from set ID to set index.
 *
 * @author Peter Abeles
 */
public class AssociateDescriptionHashSets<Desc> extends BaseAssociateSets<Desc> {

	// Regular association algorithm
	final AssociateDescription<Desc> associator;

	/** Mapping from set ID to set array index */
	TIntIntMap setToIndex = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

	/**
	 * Provides the association algorithm and the descriptor type
	 *
	 * @param associator Association algorithm
	 * @param type Type of descriptor
	 */
	public AssociateDescriptionHashSets( AssociateDescription<Desc> associator, Class<Desc> type ) {
		super(associator, type);
		this.associator = associator;
	}

	/**
	 * Override the default behavior which assumes there's a one-to-one match between index and set ID
	 */
	@Override public void initialize( int numberOfSets ) {
		assert (numberOfSets > 0);

		countSrc = 0;
		countDst = 0;
		unassociatedDst.reset();
		unassociatedDst.reset();
		sets.reset();
		setToIndex.clear();
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 *
	 * @param description The feature's description. This reference is saved internally.
	 * @param set The set the feature belongs to.
	 */
	public void addSource( Desc description, int set ) {
		final SetStruct ss = lookupSetByID(set);

		ss.src.add(description);
		ss.indexSrc.add(countSrc++);
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 *
	 * @param description The feature's description. This reference is saved internally.
	 * @param set The set the feature belongs to.
	 */
	public void addDestination( Desc description, int set ) {
		final SetStruct ss = lookupSetByID(set);
		ss.dst.add(description);
		ss.indexDst.add(countDst++);
	}

	private SetStruct lookupSetByID( int set ) {
		final SetStruct ss;
		int setIndex = setToIndex.get(set);
		if (setIndex==-1) {
			setToIndex.put(sets.size, set);
			ss = sets.grow();
		} else {
			ss = sets.get(setIndex);
		}
		return ss;
	}

	/**
	 * Associates each set of features independently then puts them back into a single list for output
	 */
	@Override public void associate() {
		if (sets.size <= 0)
			throw new IllegalArgumentException("You must initialize first with the number of sets");

		// reset data structures
		matches.reset();
		unassociatedSrc.reset();
		unassociatedDst.reset();

		// Compute results inside each set and copy them over into the output structure
		for (int setIdx = 0; setIdx < sets.size; setIdx++) {
			SetStruct set = sets.get(setIdx);

			// Associate features inside this set
			associator.setSource(set.src);
			associator.setDestination(set.dst);
			associator.associate();

			saveSetAssociateResults(set);
		}
	}
}
