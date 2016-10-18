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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Base class for algorithms which consider all possible associations but perform a quick distance calculation
 * to remove unlikely matches before computing the more expensive fit score between two descriptions.  The
 * maxDistance is the upper limit and features with a distance greater than maxDistance are rejected.  Maximum
 * error is exclusive and a match must have an error which is less than the max error.
 *
 * By default the max-distance and max error are set to Double.MAX_VALUE.
 *
 * @author Peter Abeles
 */
public abstract class BaseAssociateLocation2DFilter<D> implements AssociateDescription2D<D> {
	// computes association score
	private ScoreAssociation<D> scoreAssociation;

	// maximum allowed distance from the epipolar line
	protected double maxDistance = Double.MAX_VALUE;

	// the largest allowed error
	protected double maxError = Double.MAX_VALUE;

	// input lists
	private FastQueue<Point2D_F64> locationSrc;
	private FastQueue<D> descSrc;
	private FastQueue<Point2D_F64> locationDst;
	private FastQueue<D> descDst;

	// list of source features not associated
	private GrowQueue_I32 unassociatedSrc = new GrowQueue_I32();

	// list of features that have been matched with each other
	private FastQueue<AssociatedIndex> matched = new FastQueue<>(10, AssociatedIndex.class, true);

	// creates a list of unassociated features from the list of matches
	private FindUnassociated unassociated = new FindUnassociated();

	// is backwards validation performed during association?
	private boolean backwardsValidation = true;

	/**
	 * Specifies score mechanism
	 *
	 * @param scoreAssociation How features are scored.
	 * @param backwardsValidation Require that matches are mutual in forward/backwards directions
	 * @param maxError Maximum allowed association error
	 */
	protected BaseAssociateLocation2DFilter( ScoreAssociation<D> scoreAssociation ,
											 boolean backwardsValidation ,
											 double maxError )
	{
		this.scoreAssociation = scoreAssociation;
		this.backwardsValidation = backwardsValidation;
		this.maxError = maxError;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	@Override
	public void setSource(FastQueue<Point2D_F64> location, FastQueue<D> descriptions) {
		if( location.size() != descriptions.size() )
			throw new IllegalArgumentException("The two lists must be the same size");

		this.locationSrc = location;
		this.descSrc = descriptions;
	}

	@Override
	public void setDestination(FastQueue<Point2D_F64> location, FastQueue<D> descriptions) {
		if( location.size() != descriptions.size() )
			throw new IllegalArgumentException("The two lists must be the same size");

		this.locationDst = location;
		this.descDst = descriptions;
	}

	protected abstract void setActiveSource( Point2D_F64 p );

	protected abstract double computeDistanceToSource( Point2D_F64 p );

	@Override
	public void associate() {

		unassociatedSrc.reset();
		matched.reset();

		for( int i = 0; i < locationSrc.size(); i++ ) {
			Point2D_F64 p_s = locationSrc.get(i);
			D d_s = descSrc.get(i);
			setActiveSource(p_s);

			double bestScore = maxError;
			int bestIndex = -1;

			// find the best match in destination list
			for( int j = 0; j < locationDst.size(); j++ ) {
				D d_d = descDst.get(j);

				// compute distance between the two features
				double distance = computeDistanceToSource(locationDst.get(j));
				if( distance > maxDistance )
					continue;

				double score = scoreAssociation.score(d_s,d_d);
				if( score < bestScore ) {
					bestScore = score;
					bestIndex = j;
				}
			}

			if( bestIndex == -1 ) {
				unassociatedSrc.add(i);
				continue;
			}

			if( backwardsValidation &&  !backwardsValidation(i, bestIndex)) {
				unassociatedSrc.add(i);
				continue;
			}

			AssociatedIndex m = matched.grow();
			m.src = i;
			m.dst = bestIndex;
			m.fitScore = bestScore;
		}
	}

	/**
	 * Finds the best match for an index in destination and sees if it matches the source index
	 *
	 * @param indexSrc The index in source being examined
	 * @param bestIndex Index in dst with the best fit to source
	 * @return true if a match was found and false if not
	 */
	private boolean backwardsValidation(int indexSrc, int bestIndex) {
		double bestScoreV = maxError;
		int bestIndexV = -1;

		D d_forward = descDst.get(bestIndex);
		setActiveSource(locationDst.get(bestIndex));

		for( int j = 0; j < locationSrc.size(); j++ ) {

			// compute distance between the two features
			double distance = computeDistanceToSource(locationSrc.get(j));
			if( distance > maxDistance )
				continue;

			D d_v = descSrc.get(j);

			double score = scoreAssociation.score(d_forward,d_v);
			if( score < bestScoreV ) {
				bestScoreV = score;
				bestIndexV = j;
			}
		}

		return bestIndexV == indexSrc;
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matched;
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassociatedSrc;
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matched,locationDst.size());
	}

	@Override
	public void setThreshold(double score) {
		maxError = score;
	}

	@Override
	public MatchScoreType getScoreType() {
		return scoreAssociation.getScoreType();
	}

	@Override
	public boolean uniqueSource() {
		return true;
	}

	@Override
	public boolean uniqueDestination() {
		return false;
	}
}
