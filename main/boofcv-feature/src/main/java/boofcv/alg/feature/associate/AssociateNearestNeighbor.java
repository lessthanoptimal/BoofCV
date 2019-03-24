/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
 * as a lump using {@link NearestNeighbor#setPoints(List, boolean)} while destination features
 * are matched one at time using {@link NearestNeighbor.Search#findNearest(Object, double, NnData)}.
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
public abstract class AssociateNearestNeighbor<D>
		implements AssociateDescription<D>
{
	// Nearest Neighbor algorithm and storage for the results
	NearestNeighbor<D> alg;

	// list of features in destination set that are to be searched for in the source list
	FastQueue<D> listDst;

	int sizeSrc;

	// should the square root of the distance be used instead of the actual distance
	boolean ratioUsesSqrt =true;

	// A match is only accepted if the score of the second match over the best match is less than this value
	double scoreRatioThreshold =1.0;

	// List of final associated points
	protected final FastQueue<AssociatedIndex> matchesAll = new FastQueue<>(100, AssociatedIndex.class, true);

	// creates a list of unassociated features from the list of matches
	private FindUnassociated unassociated = new FindUnassociated();

	// maximum distance away two points can be
	double maxDistance = -1;

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
	public FastQueue<AssociatedIndex> getMatches() {
		return matchesAll;
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassociated.checkSource(matchesAll,sizeSrc);
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matchesAll,listDst.size());
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
