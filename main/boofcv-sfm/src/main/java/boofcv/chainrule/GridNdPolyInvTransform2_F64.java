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

/// Inverts a [GridNdPolyTransform2_F64] numerically instead of learning the reverse map. Given a point
/// the forward transform produced, it recovers the point that produced it.
///
/// The forward map is `p + correction(p)`, so the fixed point iteration is
/// `p <- p + (target - forward(p))`. It contracts while the correction changes slowly compared to the
/// point itself, which holds for lens distortion of the size a camera actually has.
///
/// The trade against a learned reverse grid is exactness for reliability. This composes with the
/// forward map to within [#tol] by construction, where a learned grid only agrees to whatever the two
/// independent fits happen to share. What it gives up is the guarantee of producing an answer: a grid
/// surface need not be invertible, and where it is not this stops on [#maxIterations] with whatever it
/// reached. [#converged()  says which happened, so a caller can tell the two apart
@Accessors(chain = true, fluent = true)
public class GridNdPolyInvTransform2_F64 implements Point2Transform2_F64 {
	/// How close the forward map has to land before the answer is accepted, in the units of the input
	@Getter @Setter double tol = 1e-8;

	/// Giving up point. Reaching it means the iteration was not contracting here
	@Getter @Setter int maxIterations = 20;

	/// Whether the last call reached [#tol] rather than running out of iterations
	@Getter boolean converged;

	/// How many iterations the last call used
	@Getter int iterations;

	private final Point2Transform2_F64 forward;
	private final Point2D_F64 predicted = new Point2D_F64();

	public GridNdPolyInvTransform2_F64( Point2Transform2_F64 forward ) {
		this.forward = forward;
	}

	@Override public void compute( double x, double y, Point2D_F64 out ) {
		// the correction is small, so where it landed is a good guess at where it started
		out.setTo(x, y);

		converged = false;
		for (iterations = 1; iterations <= maxIterations; iterations++) {
			forward.compute(out.x, out.y, predicted);

			double errorX = x - predicted.x;
			double errorY = y - predicted.y;

			out.x += errorX;
			out.y += errorY;

			if (Math.abs(errorX) <= tol && Math.abs(errorY) <= tol) {
				converged = true;
				return;
			}
		}
	}

	@Override public Point2Transform2_F64 copyConcurrent() {
		return new GridNdPolyInvTransform2_F64(forward.copyConcurrent())
				.tol(tol).maxIterations(maxIterations);
	}
}
