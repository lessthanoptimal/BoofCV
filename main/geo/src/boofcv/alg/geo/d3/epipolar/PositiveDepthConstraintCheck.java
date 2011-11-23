/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.epipolar;

import georegression.geometry.GeometryMath_F64;
import georegression.metric.ClosestPoint3D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * <p>
 * Given two views of the same point and a known 3D transform checks to see if the point is in front
 * of both cameras.  This is the positive depth constraint.  A class is provided instead of a function
 * to reduce computational overhead each time the function is called.  Memory only needs to be
 * declared once.
 * </p>
 *
 * <p>
 * The triangulated point is found by computing the closest point on both rays to each other.  Observations
 * must be in calibrated coordinates.
 * </p>
 *
 * @author Peter Abeles
 */
public class PositiveDepthConstraintCheck {
	// pre-declare all data structure for faster bulk computations

	// the origin of the 3D coordinate system
	Point3D_F64 origin = new Point3D_F64();
	// Value of 't' in parametric line equation for the closest points
	double closestPoints[] = new double[2];

	// line from camera A to object
	LineParametric3D_F64 lineA = new LineParametric3D_F64();
	// line from camera B to object
	LineParametric3D_F64 lineB = new LineParametric3D_F64();

	/**
	 * Checks to see if a single point meets the constraint.
	 *
	 * @param viewA View of the 3D point from the first camera.  Calibrated coordinates.
	 * @param viewB View of the 3D point from the second camera.  Calibrated coordinates.
	 * @param fromBtoA Transform from the B to A camera frame.
	 * @return If the triangulated point appears in front of both cameras.
	 */
	public boolean checkConstraint( Point2D_F64 viewA , Point2D_F64 viewB , Se3_F64 fromBtoA ) {
		// vector point from each camera's center to the point
		lineA.getSlope().set(viewA.x,viewA.y,1.0);
		lineB.getSlope().set(viewB.x,viewB.y,1.0);

		// find location of camera B's origin in the first camera's frame
		SePointOps_F64.transform(fromBtoA,origin,lineB.getPoint());
		// adjust camera B's pointing vector
		GeometryMath_F64.mult(fromBtoA.getR(), lineB.getSlope(), lineB.getSlope());


		if( !ClosestPoint3D_F64.closestPoints(lineA, lineB, closestPoints) )
			return false;

		return( closestPoints[0] > 0 && closestPoints[1] > 0 );
	}
}
