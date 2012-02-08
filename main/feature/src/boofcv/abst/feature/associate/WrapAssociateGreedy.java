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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import pja.sorting.QuickSelectArray;


/**
 * Wrapper around algorithms contained inside of {@link boofcv.alg.feature.associate.AssociateGreedy}.
 *
 * @author Peter Abeles
 */
public class WrapAssociateGreedy<T> implements GeneralAssociation<T> {

	AssociateGreedy<T> alg;

	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(10,AssociatedIndex.class,true);
	int indexes[] = new int[1];
	int maxAssociations;

	/**
	 *
	 * @param alg
	 * @param maxAssociations Maximum number of allowed associations.  If -1 then all are returned.
	 */
	public WrapAssociateGreedy( AssociateGreedy<T> alg , int maxAssociations ) {
		this.alg = alg;
		this.maxAssociations = maxAssociations;
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public void associate(FastQueue<T> listSrc, FastQueue<T> listDst) {
		alg.associate(listSrc,listDst);

		int pairs[] = alg.getPairs();
		double score[] = alg.getFitQuality();

		if( indexes.length < listSrc.size )
			indexes = new int[ listSrc.size ];

		// set up the index for sorting and see how many matches where found
		int numMatches = 0;
		for( int i = 0; i < listSrc.size; i++ ) {
			indexes[i] = i;
			if( pairs[i] >= 0 )
				numMatches++;
		}

		matches.reset();
		if( maxAssociations <= 0 || numMatches <= maxAssociations ) {
			// copy copy all the matches
			for( int i = 0; i < listSrc.size; i++ ) {
				int dst = pairs[i];
				if( dst >= 0 )
					matches.pop().setAssociation(i,dst);
			}
			
		} else {
			QuickSelectArray.selectIndex(score,maxAssociations,listSrc.size,indexes);
			for( int i = 0; i < maxAssociations; i++ ) {
				int src = indexes[i];
				int dst = pairs[src];
				if( dst == -1 ) {
					break;
				}
				matches.pop().setAssociation(src,dst);
			}
		}
	}
}
