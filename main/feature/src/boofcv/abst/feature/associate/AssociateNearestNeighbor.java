/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.FastQueue;
import boofcv.struct.GrowingArrayInt;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches features using a {@link NearestNeighbor} search with the source being the data base that is searched inside
 * of.  This is useful when the source is set once, but the destination changes.  Unique source association is enforced
 * by discarding any matches which have the same source.
 *
 * @author Peter Abeles
 */
public class AssociateNearestNeighbor<D extends TupleDesc_F64>
		implements AssociateDescription<D>
{
	// Nearest Neighbor algorithm and storage for the results
	NearestNeighbor<Integer> alg;
	NnData<Integer> result = new NnData<Integer>();

	// list of features in destination set that are to be searched for in the source list
	FastQueue<D> listDst;

	// List of indexes.  Passed in as data associated with source points
	FastQueue<Integer> indexes = new FastQueue<Integer>(0,Integer.class,false);
	// arrays that store results and scores for matches with source list
	int matchIndexes[] = new int[0];
	double matchScores[] = new double[0];

	// storage for source points
	List<double[]> src = new ArrayList<double[]>();

	// List of final associated points
	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(100,AssociatedIndex.class,true);

	// list of indexes in source which are unassociated
	GrowingArrayInt unassociatedSrc = new GrowingArrayInt();

	public AssociateNearestNeighbor(NearestNeighbor<Integer> alg , int featureDimension ) {
		this.alg = alg;
		alg.init(featureDimension);
	}

	@Override
	public void setSource(FastQueue<D> listSrc) {
		// grow the index list while copying over old values
		if( indexes.data.length < listSrc.size() ) {
			Integer a[] = new Integer[listSrc.size()];
			System.arraycopy(indexes.data,0,a,0,indexes.data.length);
			for( int i = indexes.data.length; i < a.length; i++ ) {
				a[i] = i;
			}
			indexes.data = a;
			indexes.size = a.length;
		} else {
			indexes.size = listSrc.size();
		}

		// put all the arrays into a list
		src.clear();
		for( int i = 0; i < listSrc.size; i++ ) {
			src.add(listSrc.data[i].value);
		}

		alg.setPoints(src,indexes.toList());
	}

	@Override
	public void setDestination(FastQueue<D> listDst) {
		this.listDst = listDst;
	}

	@Override
	public void associate() {

		// grow and initialize data structures
		if( matchIndexes.length < src.size() ) {
			matchIndexes = new int[ src.size() ];
			matchScores = new double[ src.size() ];
		}
		for( int i = 0; i < src.size(); i++ ) {
			matchIndexes[i] = -1;
		}

		for( int i = 0; i < listDst.size; i++ ) {
			if( !alg.findNearest(listDst.data[i].value,-1,result) )
				continue;
			// get the index of the source feature
			int indexSrc = result.data;
			int prev = matchIndexes[indexSrc];
			// if nothing else is associated there save this index
			// if another match was already found, mark it as having multiple matches
			if( prev == -1 ) {
				matchIndexes[indexSrc] = i; // single match
				matchScores[indexSrc] = result.distance;
			} else
				matchIndexes[indexSrc] = -2; // multiple matches
		}

		// return a list of unique source associations
		unassociatedSrc.reset();
		matches.reset();
		for( int i = 0; i < src.size(); i++ ) {
			int indexDst = matchIndexes[i];
			if( indexDst >= 0 ) {
				matches.grow().setAssociation(i,indexDst,matchScores[i]);
			} else {
				unassociatedSrc.add(i);
			}
		}
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public GrowingArrayInt getUnassociatedSource() {
		return unassociatedSrc;
	}
}
