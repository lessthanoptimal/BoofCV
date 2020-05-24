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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Feature set aware association algorithm. It works by breaking sorting descriptors into their sets and then
 * performing association independently. The matches are then combined together again into a single list.
 *
 * @author Peter Abeles
 */
public class AssociateDescriptionSets<Desc> implements Associate {

	// Regular association algorithm
	final AssociateDescription<Desc> associator;
	// Type of descriptor
	final Class<Desc> type;

	// Stores sorted descriptors by sets
	final FastQueue<SetStruct> sets;
	// Number of source and destination descriptors added in all sets combined
	int countSrc, countDst;

	// Output data structures
	final FastQueue<AssociatedIndex> matches;
	final GrowQueue_I32 unassociatedSrc = new GrowQueue_I32();
	final GrowQueue_I32 unassociatedDst = new GrowQueue_I32();

	/**
	 * Provides the association algorithm and the descriptor type
	 * @param associator Association algorithm
	 * @param type Type of descriptor
	 */
	public AssociateDescriptionSets(AssociateDescription<Desc> associator, Class<Desc> type) {
		this.associator = associator;
		this.type = type;

		// Declare this now since type information is now known
		this.matches = new FastQueue<>(AssociatedIndex::new);
		sets = new FastQueue<>(SetStruct::new,SetStruct::reset);
	}

	/**
	 * Specifies the number of sets and resets all internal data structures. This must be called before any other
	 * function.
	 */
	public void initialize(int numberOfSets ) {
		assert(numberOfSets>0);

		countSrc = 0;
		countDst = 0;
		unassociatedDst.reset();
		unassociatedDst.reset();
		sets.reset();
		sets.resize(numberOfSets);
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
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	public void addSource( Desc description , int set )
	{
		final SetStruct ss = sets.data[set];
		ss.src.add(description);
		ss.indexSrc.add(countSrc++);
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	public void addDestination( Desc description , int set )
	{
		final SetStruct ss = sets.data[set];
		ss.dst.add(description);
		ss.indexDst.add(countDst++);
	}

	/**
	 * Associates each set of features independently then puts them back into a single list for output
	 */
	@Override
	public void associate() {
		int before; // used to store the size of the structure from a previous iteration
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

			// Copy the results from being local to this set into the original input indexes
			FastAccess<AssociatedIndex> setMatches = associator.getMatches();
			before = matches.size;
			matches.resize(matches.size+setMatches.size);
			for (int assocIdx = 0; assocIdx < setMatches.size; assocIdx++) {
				AssociatedIndex sa = setMatches.get(assocIdx);
				int inputIdxSrc = set.indexSrc.data[sa.src];
				int inputIdxDst = set.indexDst.data[sa.dst];
				matches.data[before+assocIdx].setAssociation(inputIdxSrc,inputIdxDst,sa.fitScore);
			}

			// Copy unassociated indexes over and updated indexes to input indexes
			GrowQueue_I32 setUnassociatedSrc = associator.getUnassociatedSource();
			before = unassociatedSrc.size;
			unassociatedSrc.resize(unassociatedSrc.size+setUnassociatedSrc.size);
			for (int i = 0; i < setUnassociatedSrc.size; i++) {
				unassociatedSrc.data[before+i] = set.indexSrc.data[setUnassociatedSrc.get(i)];
			}
			GrowQueue_I32 setUnassociatedDst = associator.getUnassociatedDestination();
			before = unassociatedDst.size;
			unassociatedDst.resize(unassociatedDst.size+setUnassociatedDst.size);
			for (int i = 0; i < setUnassociatedDst.size; i++) {
				unassociatedDst.data[before+i] = set.indexDst.data[setUnassociatedDst.get(i)];
			}
		}
	}

	@Override public FastAccess<AssociatedIndex> getMatches() {return matches;}
	@Override public GrowQueue_I32 getUnassociatedSource() {return unassociatedSrc;}
	@Override public GrowQueue_I32 getUnassociatedDestination() {return unassociatedDst;}
	@Override public void setMaxScoreThreshold(double score) {associator.setMaxScoreThreshold(score);}
	@Override public MatchScoreType getScoreType() {return associator.getScoreType();}
	@Override public boolean uniqueSource() {return associator.uniqueSource();}
	@Override public boolean uniqueDestination() {return associator.uniqueDestination();}

	/**
	 * Stores data specific to a feature set
	 */
	class SetStruct {
		// Descriptors inside this set
		FastArray<Desc> src = new FastArray<>(type);
		FastArray<Desc> dst = new FastArray<>(type);

		// index of the descriptors in the input list
		GrowQueue_I32 indexSrc = new GrowQueue_I32();
		GrowQueue_I32 indexDst = new GrowQueue_I32();

		public void reset() {
			src.reset();
			dst.reset();
			indexSrc.reset();
			indexDst.reset();
		}
	}

}
