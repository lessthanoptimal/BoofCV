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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;
import georegression.struct.point.Point2D_F64;

/**
 * Two features are only considered for association if they are within the specified max distance
 * of each other.  Every possible association is considered, but only features that are close to
 * each other are scored.
 *
 * @author Peter Abeles
 */
public class AssociateMaxDistanceNaive<D> extends BaseAssociateLocation2DFilter<D>
	implements AssociateMaxDistance<D>
{

	// location of the source pixel
	private Point2D_F64 src;

	// max distance before being squared
	protected double maxDistanceNotSquared;

	/**
	 * Specifies score mechanism
	 *
	 * @param scoreAssociation How features are scored.
	 */
	public AssociateMaxDistanceNaive(ScoreAssociation<D> scoreAssociation,
									 boolean backwardsValidation,
									 double maxError )
	{
		super(scoreAssociation,backwardsValidation,maxError);
	}

	public AssociateMaxDistanceNaive(ScoreAssociation<D> scoreAssociation,
									 boolean backwardsValidation,
									 double maxError ,
									 double maxDistance )
	{
		super(scoreAssociation,backwardsValidation,maxError);
		setMaxDistance(maxDistance);
	}

	@Override
	public double getMaxDistance() {
		return maxDistanceNotSquared;
	}

	@Override
	public void setMaxDistance(double maxDistance) {
		super.setMaxDistance(maxDistance*maxDistance);
		this.maxDistanceNotSquared = maxDistance;
	}

	@Override
	protected void setActiveSource(Point2D_F64 p) {
		this.src = p;
	}

	@Override
	protected double computeDistanceToSource(Point2D_F64 p) {
		return p.distance2(src);
	}
}
