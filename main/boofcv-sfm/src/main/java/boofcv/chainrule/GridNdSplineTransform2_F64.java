/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.ddogleg.chainrule.grid.GridCoefND;
import org.ddogleg.chainrule.grid.GridNdHelper;

/// [GridNdPolyTransform2_F64] with each knot read as a spline rather than a polynomial. The grid holds
/// the same coefficients either way, so the range they are spread across has to be given here: the grid
/// does not carry it and reading the samples at the wrong places is silent
@Accessors(chain = true, fluent = true)
public class GridNdSplineTransform2_F64 implements Point2Transform2_F64 {
	public GridNdHelper helper = new GridNdHelper();

	/// Value the splines are being evaluated at
	@Getter @Setter public double value;

	public GridNdSplineTransform2_F64( GridCoefND grid, int width, int height,
									   double valueMin, double valueMax ) {
		if (grid.config.numOut != 2)
			throw new IllegalArgumentException("Requires 2 outputs not " + grid.config.numOut);
		if (grid.config.coefs < 2)
			throw new IllegalArgumentException("A spline needs at least 2 samples, not " + grid.config.coefs);

		helper.configure(grid, width, height);
		helper.configureSpline(valueMin, valueMax);
	}

	@Override public void compute( double x, double y, Point2D_F64 out ) {
		helper.coorToGrid(x, y);
		helper.gridToWeights();
		out.x = x + helper.blendSpline(value, 0);
		out.y = y + helper.blendSpline(value, 1);
	}

	@Override public Point2Transform2_F64 copyConcurrent() {
		return new GridNdSplineTransform2_F64(helper.getGrid(), helper.width, helper.height,
				helper.valueMin, helper.valueMax).value(value);
	}
}
