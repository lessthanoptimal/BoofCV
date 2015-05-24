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
import org.ddogleg.optimization.functions.FunctionNtoN;

import java.util.List;

/**
 * Gradient of {@link CornerFitFunction_F64}
 *
 * @author Peter Abeles
 */
public class CornerFitGradient_F64 implements FunctionNtoN {

	List<PointGradient_F64> points;

	public void setPoints( List<PointGradient_F64> points ) {
		this.points = points;
	}

	@Override
	public int getN() {
		return 2;
	}

	@Override
	public void process(double[] input, double[] output) {
		// corner point
		double x = input[0];
		double y = input[1];

		double gradX = 0, gradY = 0;

		for (int i = 0; i < points.size(); i++) {
			PointGradient_F64 p = points.get(i);

			double rx = p.x-x;
			double ry = p.y-y;

			double r2 = rx*rx + ry*ry;
			if( r2 > 0 ) {
				double dot = rx * p.dx + ry * p.dy;

				// gradient of the cross product
				gradX += 2 * dot * (rx * dot / r2 - p.dx) / r2;
				gradY += 2 * dot * (ry * dot / r2 - p.dy) / r2;
			}
		}

		output[0] = gradX;
		output[1] = gradY;
	}
}
