/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedTriple;

import java.util.List;

/**
 * <p>
 * Initially computes the trifocal tensor using the linear method {@link TrifocalLinearPoint7}, but
 * then iteratively refines the solution to minimize algebraic error.  The solution will enforce
 * all the constraints and be geometrically valid. See page 395 in [1].
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class TrifocalAlgebraicPoint7 extends TrifocalLinearPoint7 {

	@Override
	public boolean process(List<AssociatedTriple> observations) {
		if( observations.size() < 7 )
			throw new IllegalArgumentException("At least 7 correspondences must be provided");

		// compute normalization to reduce numerical errors
		PerspectiveOps.computeNormalization(observations, N1, N2, N3);

		// compute solution in normalized pixel coordinates
		createLinearSystem(observations);

		// solve for the trifocal tensor
		solveLinearSystem();

		// enforce constraints and minimize algebraic error
		minimizeAlgebraic();

		// undo normalization
		removeNormalization();

		return true;
	}

	protected void minimizeAlgebraic() {
		// TODO Iterative refinement goes here
	}
}
