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

package boofcv.alg.shapes.corner;

import boofcv.struct.PointGradient_F64;
import org.ddogleg.optimization.functions.FunctionNtoS;

import java.util.List;

/**
 * Function which is maximized when the local gradient and the line connecting the local point to the corner point
 * is tangential.  It computes the square magnitude of the cross product.  THe cross product is all along the
 * z-axis since both vectors lie on the x-y plane.
 *
 * @author Peter Abeles
 */
public class CornerFitFunction_F64 implements FunctionNtoS {

	List<PointGradient_F64> points;

	public void setPoints( List<PointGradient_F64> points ) {
		this.points = points;
	}

	@Override
	public int getNumOfInputsN() {
		return 2;
	}

	@Override
	public double process(double[] input) {

		// corner point
		double x = input[0];
		double y = input[1];

		double sumZ2 = 0;

		for (int i = 0; i < points.size(); i++) {
			PointGradient_F64 p = points.get(i);

			double rx = p.x-x;
			double ry = p.y-y;
			double r2 = rx*rx + ry*ry;

			// cross product between the gradient and vector from corner point and the pixel
			double Z = rx*p.dy - ry*p.dx;
			sumZ2 += Z*Z/r2;
		}

		return sumZ2;
	}
}
