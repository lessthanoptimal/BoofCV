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
import georegression.geometry.UtilTrig_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Sampson first-order to geometric triangulation error.  Partially enforces epipolar constraints.  Much
 * less expensive to compute than true constrained geometric error.
 * </p>
 *
 * <p>
 * [1] Page 315 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 * 
 * @author Peter Abeles
 */
public class ResidualsTriangulateSampson implements FunctionNtoM {

	// observations of the same feature in normalized coordinates
	private List<Point2D_F64> observations;
	// Essential matrix associated with motion
	private List<DenseMatrix64F> essential;

	private Point3D_F64 point = new Point3D_F64();

	private Point3D_F64 left = new Point3D_F64();
	private Point3D_F64 right = new Point3D_F64();

	/**
	 * Configures inputs.
	 *
	 * @param observations Observations of the feature at different locations. Normalized image coordinates.
	 * @param essential Essential matrices associated with camera motion (world to camera)
	 */
	public void setObservations( List<Point2D_F64> observations,
								 List<DenseMatrix64F> essential ) {
		this.observations = observations;
		this.essential = essential;
	}

	@Override
	public int getNumOfInputsN() {
		return 3;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.size()*4;
	}

	@Override
	public void process(double[] input, double[] output) {

		point.x = input[0];
		point.y = input[1];
		point.z = input[2];

		int index = 0;
		for( int i = 0; i < observations.size(); i++ ) {
			Point2D_F64 p = observations.get(i);
			DenseMatrix64F F = essential.get(i);

			// F^T*x'
			GeometryMath_F64.multTran(F, p, left);
			// F*x
			GeometryMath_F64.mult(F, point, right);

			// Jacobian
			double j1=left.x,j2=left.y,j3=right.x,j4=right.y;

			// J*J
			double JJ = j1*j1 + j2*j2 + j3*j3 + j4*j4;

			// e=x'*F*x
			double epipolarError = UtilTrig_F64.dot(left,point);

			if( JJ == 0 ) {
				// handle pathological situation
				output[index++] = 0;
				output[index++] = 0;
				output[index++] = 0;
				output[index++] = 0;
			} else {
				// b=e/JJ
				double b = epipolarError/JJ;

				output[index++] = j1*b;
				output[index++] = j2*b;
				output[index++] = j3*b;
				output[index++] = j4*b;
			}
		}
	}
}
