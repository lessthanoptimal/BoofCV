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

package boofcv.alg.geo.calibration;

import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given a description of the calibration grid and a set of observations compute the associated Homography.
 * First a linear approximation is computed follow by non-linear refinement.  Part of calibration process
 * described in [1].
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.99
 * </p>
 *
 * @author Peter Abeles
 */
public class Zhang99ComputeTargetHomography {

	HomographyLinear4 linear = new HomographyLinear4(true);
	DenseMatrix64F found = new DenseMatrix64F(3,3);

	// location of calibration points in the target frame's in world units.
	// the z-axis is assumed to be zero
	List<Point2D_F64> worldPoints;

	public Zhang99ComputeTargetHomography( List<Point2D_F64> worldPoints ) {
		this.worldPoints = worldPoints;
	}

	/**
	 * Computes the homography from a list of detected grid points in the image.  The
	 * order of the grid points is important and must follow the expected row major
	 * starting at the top left.
	 *
	 * @param observedPoints List of ordered detected grid points in image pixels.
	 * @return True if it computed a Homography and false if it failed to compute a homography matrix.
	 */
	public boolean computeHomography( CalibrationObservation observedPoints )
	{
		if( observedPoints.size() < 4)
			throw new IllegalArgumentException("At least 4 points needed in each set of observations. " +
					" Filter these first please");

		List<AssociatedPair> pairs = new ArrayList<>();
		for( int i = 0; i < observedPoints.size(); i++ ) {
			int which = observedPoints.get(i).index;
			Point2D_F64 obs = observedPoints.get(i);

			pairs.add( new AssociatedPair(worldPoints.get(which),obs,true));
		}

		if( !linear.process(pairs,found) )
			return false;

		// todo do non-linear refinement.  Take advantage of coordinates being fixed

		return true;
	}

	/**
	 * Returns a copy of the found homography matrix.
	 * @return Homography matrix.
	 */
	public DenseMatrix64F getHomography() {
		return found.copy();
	}
}
