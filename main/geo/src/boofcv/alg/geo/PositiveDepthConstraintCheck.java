/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.factory.geo.FactoryMultiView;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * <p>
 * Given two views of the same point and a known 3D transform checks to see if the point is in front
 * of both cameras.  This is the positive depth constraint.  A class is provided instead of a function
 * to reduce computational overhead each time the function is called.  Memory only needs to be
 * declared once.   Also less chance of messing up and only checking one view instead of two views
 * if you use this class.
 * </p>
 *
 * <p>
 * COORDINATE SYSTEM: Right handed coordinate system with +z is pointing along the camera's optical axis,
 * </p>
 *
 * @author Peter Abeles
 */
public class PositiveDepthConstraintCheck {
	// algorithm used to triangulate point location
	TriangulateTwoViewsCalibrated triangulate;

	// location of triangulated point in 3D space
	Point3D_F64 P = new Point3D_F64();

	public PositiveDepthConstraintCheck(TriangulateTwoViewsCalibrated triangulate) {
		this.triangulate = triangulate;
	}

	public PositiveDepthConstraintCheck() {
		this(FactoryMultiView.triangulateTwoGeometric());
	}

	/**
	 * Checks to see if a single point meets the constraint.
	 *
	 * @param viewA View of the 3D point from the first camera.  Calibrated coordinates.
	 * @param viewB View of the 3D point from the second camera.  Calibrated coordinates.
	 * @param fromAtoB Transform from the B to A camera frame.
	 * @return If the triangulated point appears in front of both cameras.
	 */
	public boolean checkConstraint( Point2D_F64 viewA , Point2D_F64 viewB , Se3_F64 fromAtoB ) {

		triangulate.triangulate(viewA,viewB,fromAtoB,P);

		if( P.z > 0 ) {
			SePointOps_F64.transform(fromAtoB,P,P);
			return P.z > 0;
		}
		return false;
	}
}
