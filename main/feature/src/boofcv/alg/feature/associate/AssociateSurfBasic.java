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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;


/**
 * Basic algorithm for specializing association for SURF features.  Two list of features are
 * created depending on the sign of the laplacian.  These lists are associated independently then
 * combined.
 *
 * @author Peter Abeles
 */
public class AssociateSurfBasic {

	// association algorithm
	GeneralAssociation<TupleDesc_F64> assoc;

	// features segmented by laplace sign
	FastQueue<Helper> srcPositive = new FastQueue<Helper>(10,Helper.class,true);
	FastQueue<Helper> srcNegative = new FastQueue<Helper>(10,Helper.class,true);

	FastQueue<Helper> dstPositive = new FastQueue<Helper>(10,Helper.class,true);
	FastQueue<Helper> dstNegative = new FastQueue<Helper>(10,Helper.class,true);

	// stores output matches
	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(10,AssociatedIndex.class,true);

	public AssociateSurfBasic(GeneralAssociation<TupleDesc_F64> assoc) {
		this.assoc = assoc;
	}

	public void setSrc( FastQueue<SurfFeature> src ) {
		sort(src,srcPositive,srcNegative);
	}

	public void setDst( FastQueue<SurfFeature> dst ) {
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
		// find and add the matches
		matches.reset();
		assoc.associate((FastQueue)srcPositive,(FastQueue)dstPositive);
		FastQueue<AssociatedIndex> m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			AssociatedIndex a = m.data[i];
			int globalSrcIndex = srcPositive.data[a.src].index;
			int globalDstIndex = dstPositive.data[a.dst].index;
			matches.grow().setAssociation(globalSrcIndex,globalDstIndex,a.fitScore);
		}
		assoc.associate((FastQueue)srcNegative,(FastQueue)dstNegative);
		m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			AssociatedIndex a = m.data[i];
			int globalSrcIndex = srcNegative.data[a.src].index;
			int globalDstIndex = dstNegative.data[a.dst].index;
			matches.grow().setAssociation(globalSrcIndex,globalDstIndex,a.fitScore);
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
	private void sort(FastQueue<SurfFeature> input ,
					  FastQueue<Helper> pos , FastQueue<Helper> neg ) {
		pos.reset();
		neg.reset();

		for( int i = 0; i < input.size; i++ ) {
			SurfFeature f = input.get(i);
			if( f.laplacianPositive ) {
				pos.grow().wrap(f,i);
			} else {
				neg.grow().wrap(f,i);
			}
		}
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
