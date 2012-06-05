/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;

/**
 * Removes any ambiguous associations.  If there is a possible conflict the one with the lowest score
 * is selected.  If two have the same score, one is arbitrarily selected.
 *
 * @author Peter Abeles
 */
public class EnsureUniqueAssociation {

	AssociatedIndex[] bestScores = new AssociatedIndex[1];
	FastQueue<AssociatedIndex> unambiguous = new FastQueue<AssociatedIndex>(100,AssociatedIndex.class,false);

	public void process( FastQueue<AssociatedIndex> matches, int sizeDst ) {
		// initialize data structures
		if( bestScores.length < sizeDst )
			bestScores = new AssociatedIndex[sizeDst];
		else {
			for( int i = 0; i < sizeDst; i++ )
				bestScores[i] = null;
		}

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex match = matches.data[i];

			if( bestScores[match.dst] == null || match.fitScore < bestScores[match.dst].fitScore) {
				bestScores[match.dst] = match;
			}
		}

		// add the best unambiguous pairs back
		unambiguous.reset();
		for( int i = 0; i < sizeDst; i++ ) {
			if( bestScores[i] != null )
				unambiguous.add(bestScores[i]);
		}
	}

	public FastQueue<AssociatedIndex> getMatches() {
		return unambiguous;
	}
}
