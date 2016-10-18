/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.FastQueue;

/**
 * Removes any ambiguous associations.  If multiple features from the 'src' match the same feature
 * in the 'dst' then only the association with the lowest 'fitScore' is saved.
 *
 * @author Peter Abeles
 */
public class EnsureUniqueAssociation {

	// An element for each 'dst' feature.  Only the best association with the lowest score is saved here.
	AssociatedIndex[] bestScores = new AssociatedIndex[1];
	// The final output list with the best associations
	FastQueue<AssociatedIndex> unambiguous = new FastQueue<>(100, AssociatedIndex.class, false);

	/**
	 * Removes ambiguous associations.  Call {@link #getMatches()} to get the list of unambiguous
	 * matches.
	 *
	 * @param matches List of candidate associations
	 * @param sizeDst Number of 'dst' features
	 */
	public void process( FastQueue<AssociatedIndex> matches, int sizeDst ) {
		// initialize data structures
		if( bestScores.length < sizeDst )
			bestScores = new AssociatedIndex[sizeDst];

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex match = matches.data[i];

			if( bestScores[match.dst] == null || match.fitScore < bestScores[match.dst].fitScore) {
				bestScores[match.dst] = match;
			}
		}

		// add the best unambiguous pairs back
		unambiguous.reset();
		for( int i = 0; i < sizeDst; i++ ) {
			if( bestScores[i] != null ) {
				unambiguous.add(bestScores[i]);
				// clean up so that you don't have a dangling reference
				bestScores[i] = null;
			}
		}
	}

	/**
	 * List of unambiguous matches.
	 *
	 * @return list of matches
	 */
	public FastQueue<AssociatedIndex> getMatches() {
		return unambiguous;
	}
}
