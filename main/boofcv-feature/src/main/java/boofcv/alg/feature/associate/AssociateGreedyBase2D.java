/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.ConfigLength;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.FastAccess;
import pabeles.concurrency.GrowArray;

/**
 * Base class for associating image features using descriptions and 2D distance cropping. Distance is computed
 * using {@link AssociateImageDistanceFunction} interface.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class AssociateGreedyBase2D<D> extends AssociateGreedyBase<D> {
	// used to compute the distance between two points
	protected GrowArray<AssociateImageDistanceFunction> distances; // used for concurrency
	protected AssociateImageDistanceFunction distanceFunction;

	/**
	 * Maximum allowed distance between two points. If relative then it will be based on max(width,height).
	 * Inclusive .
	 */
	public final @Getter ConfigLength maxDistanceLength = ConfigLength.relative(1.0, 0.0);
	// Computed max distance in same units as `distance`
	protected double maxDistanceUnits;

	// input lists
	protected FastAccess<Point2D_F64> locationSrc;
	protected FastAccess<D> descSrc;
	protected FastAccess<Point2D_F64> locationDst;
	protected FastAccess<D> descDst;

	/**
	 * Specifies score mechanism
	 *
	 * @param scoreAssociation How features are scored.
	 */
	protected AssociateGreedyBase2D( ScoreAssociation<D> scoreAssociation,
									 AssociateImageDistanceFunction distanceFunction ) {
		super(scoreAssociation);
		this.distances = new GrowArray<>(distanceFunction::copyConcurrent);
		this.distanceFunction = distanceFunction;
	}

	/**
	 * Initializes data structures. Must call before {@link #associate()}
	 *
	 * @param imageWidth Width of input image
	 * @param imageHeight Height of input image
	 */
	public void init( int imageWidth, int imageHeight ) {
		// Compute max distance relative the the image size
		maxDistanceUnits = maxDistanceLength.compute(Math.max(imageWidth, imageHeight));
		// Adjust units so that it's the same as the distance function
		maxDistanceUnits = distanceFunction.convertPixelsToDistance(maxDistanceUnits);
	}

	public void setSource( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		if (location.size() != descriptions.size())
			throw new IllegalArgumentException("The two lists must be the same size. " + location.size + " vs " + descriptions.size);

		this.locationSrc = location;
		this.descSrc = descriptions;
	}

	public void setDestination( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		if (location.size() != descriptions.size())
			throw new IllegalArgumentException("The two lists must be the same size. " + location.size + " vs " + descriptions.size);

		this.locationDst = location;
		this.descDst = descriptions;
	}

	/**
	 * Performs association by computing a 2D score matrix and discarding matches which fail max distance check.
	 * See {@link AssociateGreedyBase} for a full description.
	 */
	public abstract void associate();
}
