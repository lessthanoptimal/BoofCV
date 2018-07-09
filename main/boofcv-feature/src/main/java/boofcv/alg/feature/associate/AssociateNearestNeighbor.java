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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * <p>Matches features using a {@link NearestNeighbor} search from DDogleg.  The source features are processed
 * as a lump using {@link NearestNeighbor#setPoints(java.util.List, boolean)} while destination features
 * are matched one at time using {@link NearestNeighbor#findNearest(Object, double, org.ddogleg.nn.NnData)}.
 * Typically the processing of source features is more expensive and should be minimized while looking up
 * destination features is fast.  Multiple matches for source features are possible while there will only
 * be a unique match for each destination feature.</p>
 *
 * <p>An optional ratio test inspired from [1] can be used. The ratio between the best and second best score is found.
 * if the difference is significant enough then the match is accepted. This this is a ratio test, knowing if the score
 * is squared is important. Please set the flag correctly. Almost always the score is Euclidean distance squared.</p>
 *
 * <p>[1] Lowe, David G. "Distinctive image features from scale-invariant keypoints."
 * International journal of computer vision 60.2 (2004): 91-110.</p>
 *
 * @author Peter Abeles
 */
public class AssociateNearestNeighbor<D>
		implements AssociateDescription<D>
{
	// Nearest Neighbor algorithm and storage for the results
	private NearestNeighbor<D> alg;
	private NnData<D> result = new NnData<>();
	private FastQueue<NnData<D>> result2 = new FastQueue(NnData.class,true);

	// list of features in destination set that are to be searched for in the source list
	private FastQueue<D> listDst;

	int sizeSrc;

	// should the square root of the distance be used instead of the actual distance
	boolean ratioUsesSqrt =true;

	// A match is only accepted if the score of the second match over the best match is less than this value
	double scoreRatioThreshold =1.0;

	// List of final associated points
	private FastQueue<AssociatedIndex> matches = new FastQueue<>(100, AssociatedIndex.class, true);

	// creates a list of unassociated features from the list of matches
	private FindUnassociated unassociated = new FindUnassociated();

	// maximum distance away two points can be
	private double maxDistance = -1;

	public AssociateNearestNeighbor(NearestNeighbor<D> alg) {
		this.alg = alg;
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

		matches.resize(listDst.size);
		matches.reset();
		if( scoreRatioThreshold >= 1.0 ) {
			// if score ratio is not turned on then just use the best match
			for (int i = 0; i < listDst.size; i++) {
				if (!alg.findNearest(listDst.data[i], maxDistance, result))
					continue;
				matches.grow().setAssociation(result.index, i, result.distance);
			}
		} else {
			for (int i = 0; i < listDst.size; i++) {
				alg.findNearest(listDst.data[i], maxDistance,2, result2);

				if( result2.size == 1 ) {
					NnData<D> r = result2.getTail();
					matches.grow().setAssociation(r.index, i, r.distance);
				} else if( result2.size == 2 ) {
					NnData<D> r0 = result2.get(0);
					NnData<D> r1 = result2.get(1);

					// ensure that r0 is the closest
					if( r0.distance > r1.distance ) {
						NnData<D> tmp = r0;
						r0 = r1;
						r1 = tmp;
					}

					double foundRatio = ratioUsesSqrt ?Math.sqrt(r0.distance)/Math.sqrt(r1.distance) :r0.distance/r1.distance;
					if( foundRatio <= scoreRatioThreshold) {
						matches.grow().setAssociation(r0.index, i, r0.distance);
					}
				} else if( result2.size != 0 ){
					throw new RuntimeException("BUG! 0,1,2 are acceptable not "+result2.size);
				}
			}
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
	public void setMaxScoreThreshold(double score) {
		this.maxDistance = score;
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

	public boolean isRatioUsesSqrt() {
		return ratioUsesSqrt;
	}

	public void setRatioUsesSqrt(boolean ratioUsesSqrt) {
		this.ratioUsesSqrt = ratioUsesSqrt;
	}

	public double getScoreRatioThreshold() {
		return scoreRatioThreshold;
	}

	public void setScoreRatioThreshold(double scoreRatioThreshold) {
		this.scoreRatioThreshold = scoreRatioThreshold;
	}
}
