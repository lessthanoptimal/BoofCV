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

package boofcv.numerics.optimization;


/**
 * Enumerated type for specifying different types of steps for use in
 * {@link boofcv.numerics.optimization.impl.TrustRegionLeastSquares}.
 * 
 * @author Peter Abeles
 */
public enum RegionStepType {
	/**
	 * Performs the optimal step along the gradient line.  While fast to compute this has
	 * very slow convergence due to it being a pure gradient descent optimization.
	 *
	 * @see boofcv.numerics.optimization.impl.CauchyStep
	 */
	CAUCHY,
	/**
	 * Dogleg that decomposes square of the Jacobian, F(x)<sup>T</sup>F(x).  If the number of functions is much
	 * larger than the number of parameters this technique can be a lot faster.  However,
	 * because of the Jacobian is squared some numerical precision is lost.
	 *
	 * @see boofcv.numerics.optimization.impl.DoglegStepFtF
	 */
	DOG_LEG_FTF,
	/**
	 * Dogleg that decomposes square of the Jacobian directly.  Has slightly higher precision
	 * than the FTF method and is faster when the number of parameters and functions are about equal.
	 *
	 * @see boofcv.numerics.optimization.impl.DoglegStepF
	 */
	DOG_LEG_F
}
