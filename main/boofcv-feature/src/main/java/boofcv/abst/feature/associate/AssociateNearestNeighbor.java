/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.associate.FindUnassociated;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Matches features using a {@link NearestNeighbor} search from DDogleg.  The source features are processed
 * as a lump using {@link NearestNeighbor#setPoints(java.util.List, boolean)} while destination features
 * are matched one at time using {@link NearestNeighbor#findNearest(Object, double, org.ddogleg.nn.NnData)}.
 * Typically the processing of source features is more expensive and should be minimized while looking up
 * destination features is fast.  Multiple matches for source features are possible while there will only
 * be a unique match for each destination feature.
 *
 * @author Peter Abeles
 */
public class AssociateNearestNeighbor<D extends TupleDesc_F64>
		implements AssociateDescription<D>
{
	// Nearest Neighbor algorithm and storage for the results
	private NearestNeighbor<TupleDesc_F64> alg;
	private NnData<TupleDesc_F64> result = new NnData<>();

	// list of features in destination set that are to be searched for in the source list
	private FastQueue<D> listDst;

	int sizeSrc;

	// List of final associated points
	private FastQueue<AssociatedIndex> matches = new FastQueue<>(100, AssociatedIndex.class, true);

	// creates a list of unassociated features from the list of matches
	private FindUnassociated unassociated = new FindUnassociated();

	// maximum distance away two points can be
	private double maxDistanceSq = -1;

	public AssociateNearestNeighbor(NearestNeighbor<TupleDesc_F64> alg , int featureDimension ) {
		this.alg = alg;
		alg.init(featureDimension);
	}

	@Override
	public void setSource(FastQueue<D> listSrc) {
		this.sizeSrc = listSrc.size;
		alg.setPoints((List)listSrc.toList(),true);
	}

	@Override
	public void setDestination(FastQueue<D> listDst) {
		this.listDst = listDst;
	}

	@Override
	public void associate() {

		matches.reset();
		for( int i = 0; i < listDst.size; i++ ) {
			if( !alg.findNearest(listDst.data[i], maxDistanceSq,result) )
				continue;
			// get the index of the source feature
			int indexSrc = result.index;
			matches.grow().setAssociation(indexSrc,i,result.distance);
		}

	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassociated.checkSource(matches,sizeSrc);
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matches,listDst.size());
	}

	@Override
	public void setThreshold(double score) {
		// NN uses Euclidean distance squared
		this.maxDistanceSq = score < 0 ? score : score*score;
	}

	@Override
	public MatchScoreType getScoreType() {
		return MatchScoreType.NORM_ERROR;
	}

	@Override
	public boolean uniqueSource() {
		return false;
	}

	@Override
	public boolean uniqueDestination() {
		return true;
	}
}
