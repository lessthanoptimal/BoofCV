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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateDescriptionArraySets;
import boofcv.abst.feature.associate.AssociateThreeDescription;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

/**
 * Associates features in three view with each other by associating each pair of images individually. Only unique
 * associations are allowed.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AssociateThreeByPairs<TD extends TupleDesc<TD>> implements AssociateThreeDescription<TD> {

	// image to image association
	protected AssociateDescriptionArraySets<TD> associator;

	// Reference to descriptions in each image
	protected FastAccess<TD> featuresA, featuresB, featuresC;
	protected DogArray_I32 setsA, setsB, setsC;

	// work space variables
	protected FastArray<TD> tmpB;
	protected FastArray<TD> tmpA;
	protected DogArray_I32 tmpSetsA = new DogArray_I32();
	protected DogArray_I32 tmpSetsB = new DogArray_I32();

	// storage for final output
	protected DogArray<AssociatedTripleIndex> matches = new DogArray<>(AssociatedTripleIndex::new);

	// used to map indexes
	protected DogArray_I32 srcToC = new DogArray_I32();

	/**
	 * Specifies which algorithms to use internally
	 *
	 * @param associator image to image association
	 */
	public AssociateThreeByPairs( AssociateDescription<TD> associator ) {
		if (!associator.uniqueDestination() || !associator.uniqueSource())
			throw new IllegalArgumentException("Both source and destination need to be unique");
		this.associator = new AssociateDescriptionArraySets<>(associator);

		tmpB = new FastArray<>(associator.getDescriptionType());
		tmpA = new FastArray<>(associator.getDescriptionType());
	}

	@Override
	public void initialize( int numberOfSets ) {
		associator.initialize(numberOfSets);
	}

	@Override
	public void setFeaturesA( FastAccess<TD> features, DogArray_I32 sets ) {
		this.featuresA = features;
		this.setsA = sets;
	}

	@Override
	public void setFeaturesB( FastAccess<TD> features, DogArray_I32 sets ) {
		this.featuresB = features;
		this.setsB = sets;
	}

	@Override
	public void setFeaturesC( FastAccess<TD> features, DogArray_I32 sets ) {
		this.featuresC = features;
		this.setsC = sets;
	}

	@Override
	public void associate() {
		sanityCheck();
		matches.reset();

		// Associate view A to view B
		UtilFeature.setSource(featuresA, setsA, associator);
		UtilFeature.setDestination(featuresB, setsB, associator);
		associator.associate();
		FastAccess<AssociatedIndex> pairs = associator.getMatches();
		tmpA.resize(pairs.size);
		tmpSetsA.resize(pairs.size);
		tmpB.resize(pairs.size);
		tmpSetsB.resize(pairs.size);
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			matches.grow().setTo(p.src, p.dst, -1);
			// indexes of tmp lists will be the same as matches
			tmpA.data[i] = featuresA.data[p.src];
			tmpB.data[i] = featuresB.data[p.dst];

			tmpSetsA.data[i] = setsA.data[p.src];
			tmpSetsB.data[i] = setsB.data[p.dst];
		}

		// Associate view B to view C, but only consider previously associated features in B
		UtilFeature.setSource(tmpB, tmpSetsB, associator);
		UtilFeature.setDestination(featuresC, setsC, associator);
		associator.associate();
		pairs = associator.getMatches();
		tmpB.resize(pairs.size);
		tmpSetsB.resize(pairs.size);
		srcToC.resize(pairs.size);
		FastArray<TD> tmpC = tmpB; // do this to make the code easier to read
		DogArray_I32 tmpSetsC = tmpSetsB;
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			// tmpSrc points to indexes in matches
			matches.get(p.src).c = p.dst;
			tmpC.data[i] = featuresC.data[p.dst];
			tmpSetsC.data[i] = setsC.data[p.dst];
			srcToC.data[i] = p.dst;           // save mapping back to original input index
		}
		// mark the unmatched as unmatched
		DogArray_I32 unsrc = associator.getUnassociatedSource();
		for (int i = 0; i < unsrc.size; i++) {
			int idx = unsrc.get(i);
			matches.get(idx).c = -1;
		}

		// Associate view C to view A but only consider features which are currently in the triple
		UtilFeature.setSource(tmpC, tmpSetsC, associator);
		UtilFeature.setDestination(tmpA, tmpSetsA, associator);
		associator.associate();
		pairs = associator.getMatches();
		for (int i = 0; i < pairs.size; i++) {
			AssociatedIndex p = pairs.get(i);
			AssociatedTripleIndex t = matches.get(p.dst);

			// index of tmpA (destination) matches the index of matches
			if (matches.get(p.dst).c != srcToC.data[p.src]) {
				t.c = -1;// mark it so that it will be pruned
			}
		}
		DogArray_I32 undst = associator.getUnassociatedDestination();
		for (int i = 0; i < undst.size; i++) {
			int idx = undst.get(i);
			matches.get(idx).c = -1;
		}

		// Prune triplets which don't match
		pruneMatches();
	}

	/**
	 * Makes sure the user didn't screw up by passing in the same lists. This could happen if they assumed a copy
	 * was being made internally.
	 */
	private void sanityCheck() {
		assert (featuresA != featuresB);
		assert (featuresA != featuresC);
		assert (featuresB != featuresC);
		assert (setsA != setsB);
		assert (setsA != setsC);
		assert (setsB != setsC);
	}

	/**
	 * Removes by swapping all elements with a 'c' index of -1
	 */
	private void pruneMatches() {
		int index = 0;
		while (index < matches.size) {
			AssociatedTripleIndex a = matches.get(index);
			// not matched. Remove it from the list by copying that last element over it
			if (a.c == -1) {
				a.setTo(matches.get(matches.size - 1));
				matches.size--;
			} else {
				index++;
			}
		}
	}

	@Override
	public DogArray<AssociatedTripleIndex> getMatches() {
		return matches;
	}

	@Override
	public void setMaxScoreThreshold( double score ) {
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
