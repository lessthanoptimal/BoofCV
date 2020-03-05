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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateThreeDescription;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Associates features in three view with each other by associating each pair of images individually. Only unique
 * associations are allowed.
 *
 * @author Peter Abeles
 */
public class AssociateThreeByPairs<Desc> implements AssociateThreeDescription<Desc> {

	// image to image association
	protected AssociateDescription<Desc> associator;

	// Reference to descriptions in each image
	protected FastQueue<Desc> featuresA,featuresB,featuresC;

	// work space varaibles
	protected FastQueue<Desc> tmpB;
	protected FastQueue<Desc> tmpA;

	// storage for final output
	protected FastQueue<AssociatedTripleIndex> matches = new FastQueue<>(AssociatedTripleIndex.class,true);

	// used to map indexes
	protected GrowQueue_I32 srcToC = new GrowQueue_I32();

	/**
	 * Specifies which algorithms to use internally
	 *
	 * @param associator image to image association
	 * @param type Type of descriptor
	 */
	public AssociateThreeByPairs(AssociateDescription<Desc> associator, Class<Desc> type ) {
		if( !associator.uniqueDestination() || !associator.uniqueSource() )
			throw new IllegalArgumentException("Both source and destination need to be unique");
		this.associator = associator;

		tmpB = new FastQueue<>(type,false);
		tmpA = new FastQueue<>(type,false);
	}

	@Override
	public void setFeaturesA(FastQueue<Desc> features) {
		this.featuresA = features;
	}

	@Override
	public void setFeaturesB(FastQueue<Desc> features) {
		this.featuresB = features;
	}

	@Override
	public void setFeaturesC(FastQueue<Desc> features) {
		this.featuresC = features;
	}

	@Override
	public void associate() {
		matches.reset();

		// Associate view A to view B
		associator.setSource(featuresA);
		associator.setDestination(featuresB);
		associator.associate();
		FastAccess<AssociatedIndex> pairs = associator.getMatches();
		tmpB.reset();
		tmpA.reset();
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			matches.grow().set(p.src,p.dst,-1);
			// indexes of tmp lists will be the same as matches
			tmpA.add(featuresA.data[p.src]);
			tmpB.add(featuresB.data[p.dst]);
		}

		// Associate view B to view C, but only consider previously associated features in B
		associator.setSource(tmpB);
		associator.setDestination(featuresC);
		associator.associate();
		pairs = associator.getMatches();
		tmpB.reset();
		srcToC.resize(pairs.size);
		FastQueue<Desc> tmpC = tmpB; // do this to make the code easier to read
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			// tmpSrc points to indexes in matches
			matches.get(p.src).c = p.dst;
			tmpC.add(featuresC.data[p.dst]);
			srcToC.data[i] = p.dst;           // save mapping back to original input index
		}
		// mark the unmatched as unmatched
		GrowQueue_I32 unsrc = associator.getUnassociatedSource();
		for (int i = 0; i < unsrc.size; i++) {
			int idx = unsrc.get(i);
			matches.get(idx).c = -1;
		}

		// Associate view C to view A but only consider features which are currently in the triple
		associator.setSource(tmpC);
		associator.setDestination(tmpA);
		associator.associate();
		pairs = associator.getMatches();
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			AssociatedTripleIndex t = matches.get(p.dst);

			// index of tmpA (destination) matches the index of matches
			if( matches.get(p.dst).c != srcToC.data[p.src]  ) {
				t.c = -1;// mark it so that it will be pruned
			}
		}
		GrowQueue_I32 undst = associator.getUnassociatedDestination();
		for (int i = 0; i < undst.size; i++) {
			int idx = undst.get(i);
			matches.get(idx).c = -1;
		}

		// Prune triplets which don't match
		pruneMatches();
	}

	/**
	 * Removes by swapping all elements with a 'c' index of -1
	 */
	private void pruneMatches() {
		int index = 0;
		while( index < matches.size ) {
			AssociatedTripleIndex a = matches.get(index);
			// not matched. Remove it from the list by copying that last element over it
			if( a.c == -1 ) {
				a.set(matches.get(matches.size-1));
				matches.size--;
			} else {
				index++;
			}
		}
	}

	@Override
	public FastQueue<AssociatedTripleIndex> getMatches() {
		return matches;
	}

	@Override
	public void setMaxScoreThreshold(double score) {
		associator.setMaxScoreThreshold(score);
	}

	@Override
	public MatchScoreType getScoreType() {
		return associator.getScoreType();
	}

	@Override
	public boolean isEachFeatureAssociatedOnlyOnce() {
		return true;
	}
}
