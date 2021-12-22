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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Computes reprojection error after triangulation for calibrated images
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class Triangulate2ViewsReprojectionMetricError {

	CameraPinhole intrinsicA;
	CameraPinhole intrinsicB;

	final Point3D_F64 Xb = new Point3D_F64();

	final Point2D_F64 pixelN = new Point2D_F64();
	final Point2D_F64 pixelX = new Point2D_F64();

	public void configure( CameraPinhole parametersA,
						   CameraPinhole parametersB ) {
		this.intrinsicA = parametersA;
		this.intrinsicB = parametersB;
	}

	/**
	 * Computes average squares error in pixel from both views.( ||a_i-x_i||^2 + ||b_i-x_i||^2 )/ 2
	 *
	 * @param a Normalized image coordinate observation in view A
	 * @param b Normalized image coordinate observation in view B
	 * @param a_to_b transform from view A to view B
	 * @param Xa 3D location in image A
	 */
	public double process( Point2D_F64 a, Point2D_F64 b, Se3_F64 a_to_b,
						   Point3D_F64 Xa ) {
		PerspectiveOps.convertNormToPixel(intrinsicA, a.x, a.y, pixelN);
		PerspectiveOps.convertNormToPixel(intrinsicA, Xa.x/Xa.z, Xa.y/Xa.z, pixelX);

		double error = pixelN.distance2(pixelX);

		a_to_b.transform(Xa, Xb);
		PerspectiveOps.convertNormToPixel(intrinsicB, b.x, b.y, pixelN);
		PerspectiveOps.convertNormToPixel(intrinsicB, Xb.x/Xb.z, Xb.y/Xb.z, pixelX);


		return (error + pixelN.distance2(pixelX))/2;
	}
}
