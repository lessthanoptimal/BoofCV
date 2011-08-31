/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.associate;

import gecv.abst.feature.associate.GeneralAssociation;
import gecv.struct.FastQueue;
import gecv.struct.feature.AssociatedIndex;
import gecv.struct.feature.SurfFeature;
import gecv.struct.feature.TupleDesc_F64;


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
	FastQueue<TupleDesc_F64> srcPositive = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);
	FastQueue<TupleDesc_F64> srcNegative = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);

	FastQueue<TupleDesc_F64> dstPositive = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);
	FastQueue<TupleDesc_F64> dstNegative = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);

	// stores output matches
	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(10,AssociatedIndex.class,true);

	public AssociateSurfBasic(GeneralAssociation<TupleDesc_F64> assoc) {
		this.assoc = assoc;
	}

	public void addSource( FastQueue<SurfFeature> src ) {
		sort(src,srcPositive,srcNegative);
	}

	public void addDest( FastQueue<SurfFeature> dst ) {
		sort(dst,dstPositive,dstNegative);
	}

	/**
	 * Swaps the source and dest feature list.  Useful when processing a sequence
	 * of images and don't want to resort everything.
	 */
	public void swapLists() {
		FastQueue<TupleDesc_F64> tmp = srcPositive;
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
		assoc.associate(srcPositive,dstPositive);
		FastQueue<AssociatedIndex> m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			matches.pop().set(m.data[i]);
		}
		assoc.associate(srcNegative,dstNegative);
		m = assoc.getMatches();
		for( int i = 0; i < m.size; i++ ) {
			matches.pop().set(m.data[i]);
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
	 */
	private void sort(FastQueue<SurfFeature> input ,
					  FastQueue<TupleDesc_F64> pos , FastQueue<TupleDesc_F64> neg ) {
		pos.reset();
		neg.reset();

		for( int i = 0; i < input.size; i++ ) {
			SurfFeature f = input.get(i);
			if( f.laplacianPositive ) {
				pos.add(f.features);
			} else {
				neg.add(f.features);
			}
		}
	}
}
