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

/**
 * Feature set aware association algorithm. It works by breaking sorting descriptors into their sets and then
 * performing association independently. The matches are then combined together again into a single list.
 *
 * @author Peter Abeles
 */
public class AssociateDescriptionArraySets<Desc> extends BaseAssociateDescriptionSets<Desc> {

	// Regular association algorithm
	final AssociateDescription<Desc> associator;

	/**
	 * Provides the association algorithm and the descriptor type
	 *
	 * @param associator Association algorithm
	 */
	public AssociateDescriptionArraySets( AssociateDescription<Desc> associator ) {
		super(associator);
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
		sets.resize(numberOfSets);
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	@Override public void addSource( Desc description, int set ) {
		final SetStruct ss = sets.data[set];
		ss.src.add(description);
		ss.indexSrc.add(countSrc++);
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	@Override public void addDestination( Desc description, int set ) {
		final SetStruct ss = sets.data[set];
		ss.dst.add(description);
		ss.indexDst.add(countDst++);
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
