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

package boofcv.alg.geo.triangulate;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Computes the depth (value of z-axis in frame A) for a single point feature given N observations
 * and N-1 rigid camera transforms.  The estimate will be optimal an algebraic sense, but not in
 * a Euclidean sense.  Two variants are provided in this class, for 2 views and N>1 views. All pixel
 * coordinates are in calibrated units.
 * </p>
 *
 * <p>
 * This linear estimation of pixel depth is done using the following equation:<br>
 * &lambda;<sub>1</sub>*hat(x<sub>2</sub>)*R*x<sub>1</sub> + hat(x<sub>2</sub>)*T = 0<br>
 * where &lambda;<sub>1</sub> is the pixel depth in the first image, x is an observation in calibrated
 * homogeneous coordinates, and (R,T) is a rigid body transformation.  Modified implementation of the algorithm
 * described in [1].
 * </p>
 *
 * <p>
 * To go from pixel depth to a 3D coordinate simply multiply the observation in calibrated (z=1) homogeneous
 * coordinates by the returned depth.
 * </p>
 *
 * <p>
 * [1] Page 267 in Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class PixelDepthLinear {

	// local variables used for temporary storage
	private DenseMatrix64F temp0 = new DenseMatrix64F(3,3);
	private Vector3D_F64 temp1 = new Vector3D_F64();
	private Vector3D_F64 temp2 = new Vector3D_F64();

	/**
	 * Computes the pixel depth from N views of the same object.  Pixel depth in the first frame.
	 *
	 * @param obs List of observations on a single feature in normalized coordinates
	 * @param motion List of camera motions.  Each index 'i' is the motion from view 0 to view i+1.
	 * @return depth of the pixels
	 */
	public double depthNView( List<Point2D_F64> obs ,
							  List<Se3_F64> motion )
	{

		double top = 0, bottom = 0;

		Point2D_F64 a = obs.get(0);
		for( int i = 1; i < obs.size(); i++ ) {
			Se3_F64 se = motion.get(i-1);
			Point2D_F64 b = obs.get(i);

			GeometryMath_F64.multCrossA(b, se.getR(), temp0);
			GeometryMath_F64.mult(temp0,a,temp1);

			GeometryMath_F64.cross(b, se.getT(), temp2);

			top += temp2.x+temp2.y+temp2.z;
			bottom += temp1.x+temp1.y+temp1.z;
		}

		return -top/bottom;
	}

	/**
	 * Computes pixel depth in image 'a' from two observations.
	 *
	 * @param a Observation in first frame.  In calibrated coordinates. Not modified.
	 * @param b Observation in second frame.  In calibrated coordinates. Not modified.
	 * @param fromAtoB Transform from frame a to frame b.
	 * @return Pixel depth in first frame. In same units as T inside of fromAtoB.
	 */
	public double depth2View( Point2D_F64 a , Point2D_F64 b , Se3_F64 fromAtoB )
	{
		DenseMatrix64F R = fromAtoB.getR();
		Vector3D_F64 T = fromAtoB.getT();

		GeometryMath_F64.multCrossA(b, R, temp0);
		GeometryMath_F64.mult(temp0,a,temp1);

		GeometryMath_F64.cross(b, T, temp2);

		return -(temp2.x+temp2.y+temp2.z)/(temp1.x+temp1.y+temp1.z);
	}

}
