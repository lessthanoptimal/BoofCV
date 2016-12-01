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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;


/**
 * Basic algorithm for specializing association for SURF features.  Two list of features are
 * created depending on the sign of the laplacian.  These lists are associated independently then
 * combined.
 *
 * @author Peter Abeles
 */
public class AssociateSurfBasic {

	// association algorithm
	AssociateDescription<TupleDesc_F64> assoc;

	// features segmented by laplace sign
	FastQueue<Helper> srcPositive = new FastQueue<>(10, Helper.class, true);
	FastQueue<Helper> srcNegative = new FastQueue<>(10, Helper.class, true);

	FastQueue<Helper> dstPositive = new FastQueue<>(10, Helper.class, true);
	FastQueue<Helper> dstNegative = new FastQueue<>(10, Helper.class, true);

	// stores output matches
	FastQueue<AssociatedIndex> matches = new FastQueue<>(10, AssociatedIndex.class, true);

	// indexes of unassociated features
	GrowQueue_I32 unassociatedSrc = new GrowQueue_I32();

	public AssociateSurfBasic(AssociateDescription<TupleDesc_F64> assoc) {
		this.assoc = assoc;
	}

	public void setSrc( FastQueue<BrightFeature> src ) {
		sort(src,srcPositive,srcNegative);
	}

	public void setDst( FastQueue<BrightFeature> dst ) {
		sort(dst,dstPositive,dstNegative);
	}

	/**
	 * Swaps the source and dest feature list.  Useful when processing a sequence
	 * of images and don't want to resort everything.
	 */
	public void swapLists() {
		FastQueue<Helper> tmp = srcPositive;
		srcPositive = dstPositive;
		dstPositive = tmp;

		tmp = srcNegative;
		srcNegative = dstNegative;
		dstNegative = tmp;
	}

	/**
	 * Associates the features together.
	 */
	public void associate()
	{
		// initialize data structures
		matches.reset();
		unassociatedSrc.reset();

		if( srcPositive.size == 0 && srcNegative.size == 0 )
			return;
		if( dstPositive.size == 0 && dstNegative.size == 0 )
			return;

		// find and add the matches
		assoc.setSource((FastQueue)srcPositive);
		assoc.setDestination((FastQueue) dstPositive);
		assoc.associate();
		FastQueue<AssociatedIndex> m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			AssociatedIndex a = m.data[i];
			int globalSrcIndex = srcPositive.data[a.src].index;
			int globalDstIndex = dstPositive.data[a.dst].index;
			matches.grow().setAssociation(globalSrcIndex,globalDstIndex,a.fitScore);
		}
		GrowQueue_I32 un = assoc.getUnassociatedSource();
		for( int i = 0; i < un.size; i++ ) {
			unassociatedSrc.add(srcPositive.data[un.get(i)].index);
		}
		assoc.setSource((FastQueue)srcNegative);
		assoc.setDestination((FastQueue) dstNegative);
		assoc.associate();
		m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			AssociatedIndex a = m.data[i];
			int globalSrcIndex = srcNegative.data[a.src].index;
			int globalDstIndex = dstNegative.data[a.dst].index;
			matches.grow().setAssociation(globalSrcIndex,globalDstIndex,a.fitScore);
		}
		un = assoc.getUnassociatedSource();
		for( int i = 0; i < un.size; i++ ) {
			unassociatedSrc.add(srcNegative.data[un.get(i)].index);
		}
	}

	/**
	 * Returns a list of found matches.
	 */
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	/**
	 * Splits the set of input features into positive and negative laplacian lists.
	 * Keep track of the feature's index in the original input list.  This is
	 * the index that needs to be returned.
	 */
	private void sort(FastQueue<BrightFeature> input ,
					  FastQueue<Helper> pos , FastQueue<Helper> neg ) {
		pos.reset();
		neg.reset();

		for( int i = 0; i < input.size; i++ ) {
			BrightFeature f = input.get(i);
			if( f.white) {
				pos.grow().wrap(f,i);
			} else {
				neg.grow().wrap(f,i);
			}
		}
	}

	public int totalDestination() {
		return dstNegative.size + dstPositive.size;
	}

	public GrowQueue_I32 getUnassociatedSrc() {
		return unassociatedSrc;
	}

	public AssociateDescription<TupleDesc_F64> getAssoc() {
		return assoc;
	}

	public static class Helper extends TupleDesc_F64
	{
		public int index;
		public void wrap( TupleDesc_F64 a , int index ) {
			this.index = index;
			value = a.value;
		}
	}
}
