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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

/**
 * Base class for set aware feature association
 *
 * @author Peter Abeles
 */
public abstract class BaseAssociateSets<Desc> implements Associate<Desc> {

	Associate<Desc> _associator;

	// Stores sorted descriptors by sets
	protected final DogArray<SetStruct> sets;

	// Number of source and destination descriptors added in all sets combined
	protected int countSrc, countDst;

	// Output data structures
	protected final DogArray<AssociatedIndex> matches;
	protected final DogArray_I32 unassociatedSrc = new DogArray_I32();
	protected final DogArray_I32 unassociatedDst = new DogArray_I32();

	/**
	 * Specifies the type of descriptor
	 */
	protected BaseAssociateSets( Associate<Desc> associator ) {
		this._associator = associator;

		// Declare this now since type information is now known
		this.matches = new DogArray<>(AssociatedIndex::new);
		this.sets = new DogArray<>(this::newSetStruct, SetStruct::reset);
	}

	protected SetStruct newSetStruct() {
		return new SetStruct();
	}

	/**
	 * Removes all data for the source descriptors.
	 */
	public void clearSource() {
		unassociatedSrc.reset();
		countSrc = 0;
		for (int i = 0; i < sets.size; i++) {
			sets.get(i).src.reset();
			sets.get(i).indexSrc.reset();
		}
	}

	/**
	 * Removes all data for the destination descriptors.
	 */
	public void clearDestination() {
		unassociatedDst.reset();
		countDst = 0;
		for (int i = 0; i < sets.size; i++) {
			sets.get(i).dst.reset();
			sets.get(i).indexDst.reset();
		}
	}

	/**
	 * After associating a set run association these processes and saves the results
	 */
	protected void saveSetAssociateResults( SetStruct set ) {
		// used to store the size of the structure from a previous iteration
		int before = matches.size;
		// Copy the results from being local to this set into the original input indexes
		FastAccess<AssociatedIndex> setMatches = _associator.getMatches();

		matches.resize(matches.size + setMatches.size);
		for (int assocIdx = 0; assocIdx < setMatches.size; assocIdx++) {
			AssociatedIndex sa = setMatches.get(assocIdx);
			int inputIdxSrc = set.indexSrc.data[sa.src];
			int inputIdxDst = set.indexDst.data[sa.dst];
			matches.data[before + assocIdx].setTo(inputIdxSrc, inputIdxDst, sa.fitScore);
		}

		// Copy unassociated indexes over and updated indexes to input indexes
		DogArray_I32 setUnassociatedSrc = _associator.getUnassociatedSource();
		before = unassociatedSrc.size;
		unassociatedSrc.extend(before + setUnassociatedSrc.size);
		for (int i = 0; i < setUnassociatedSrc.size; i++) {
			unassociatedSrc.data[before + i] = set.indexSrc.data[setUnassociatedSrc.get(i)];
		}
		DogArray_I32 setUnassociatedDst = _associator.getUnassociatedDestination();
		before = unassociatedDst.size;
		unassociatedDst.extend(before + setUnassociatedDst.size);
		for (int i = 0; i < setUnassociatedDst.size; i++) {
			unassociatedDst.data[before + i] = set.indexDst.data[setUnassociatedDst.get(i)];
		}
	}

	@Override public FastAccess<AssociatedIndex> getMatches() {return matches;}

	@Override public DogArray_I32 getUnassociatedSource() {return unassociatedSrc;}

	@Override public DogArray_I32 getUnassociatedDestination() {return unassociatedDst;}

	@Override public void setMaxScoreThreshold( double score ) {_associator.setMaxScoreThreshold(score);}

	@Override public MatchScoreType getScoreType() {return _associator.getScoreType();}

	@Override public boolean uniqueSource() {return _associator.uniqueSource();}

	@Override public boolean uniqueDestination() {return _associator.uniqueDestination();}

	@Override public Class<Desc> getDescriptionType() {return _associator.getDescriptionType();}

	/**
	 * Stores data specific to a feature set
	 */
	protected class SetStruct {
		// Descriptors inside this set
		public FastArray<Desc> src = new FastArray<>(_associator.getDescriptionType());
		public FastArray<Desc> dst = new FastArray<>(_associator.getDescriptionType());

		// index of the descriptors in the input list
		public DogArray_I32 indexSrc = new DogArray_I32();
		public DogArray_I32 indexDst = new DogArray_I32();

		public void reset() {
			src.reset();
			dst.reset();
			indexSrc.reset();
			indexDst.reset();
		}
	}
}
