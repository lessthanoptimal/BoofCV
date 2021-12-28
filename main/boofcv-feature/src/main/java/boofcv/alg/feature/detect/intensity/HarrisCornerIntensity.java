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

package boofcv.alg.feature.detect.intensity;

/**
 * <p>
 * The Harris corner detector [1] is similar to the {@link ShiTomasiCornerIntensity} but avoids computing the eigenvalues
 * directly. In theory this should be more computationally efficient.
 * </p>
 *
 * <p>
 * corner = det(D) + k*trace(D)<sup>2</sup><br>
 * where D is the deformation matrix (see {@link GradientCornerIntensity}), and k is a tunable scalar.
 * </p>
 *
 * <p>
 * k typically has a small value, for example 0.04.
 * </p>
 *
 * @author Peter Abeles
 */
public interface HarrisCornerIntensity {
	/**
	 * Returns the value of the tuning parameter.
	 *
	 * @return tuning parameter
	 */
	float getKappa();

	/**
	 * Sets the tuning parameter.
	 *
	 * @param kappa parameter
	 */
	void setKappa( float kappa );
}
