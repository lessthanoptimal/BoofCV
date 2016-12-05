/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.flow.HornSchunckPyramid}
 *
 * @author Peter Abeles
 */
public class ConfigBroxWarping implements Configuration {

	/**
	 * Brightness difference weighting factor.  Larger values which prefer a smooth flow.
	 */
	public float alpha = 0.04f;

	/**
	 * Gradient difference weighting factor.  Larger values which prefer a smooth flow.
	 */
	public float gamma = 0.03f;

	/**
	 * SOR relaxation parameter.  0 < w < 2.  Recommended default is 1.9
	 */
	public float SOR_RELAXATION = 1.9f;

	/**
	 * Number of iterations in the outer loop
	 */
	public int numOuter = 10;
	/**
	 * Number of iterations in the inner loop
	 */
	public int numInner = 1;

	/**
	 * Maximum allowed iterations for SOR
	 */
	public int maxIterationsSor = 100;

	/**
	 * Convergence tolerance for SOR loop.  Specified in per pixel error.
	 */
	public float convergeToleranceSor = 1e-5f;
	/**
	 * Change in scale between each layer.  Try 0.75
	 */
	public double pyrScale = 0.75;
	/**
	 * Amount of gaussian blur applied to each layer in the pyramid.  If sigma &le; 0 then no blur is applied.
	 */
	public double pyrSigma = 0.5;
	/**
	 * Maximum number of layers in the pyramid
	 */
	public int pyrMaxLayers = 100;

	/**
	 * Type of interpolation used.  Bilinear recommended
	 */
	public InterpolationType interpolation = InterpolationType.BILINEAR;

	@Override
	public void checkValidity() {}

	public ConfigBroxWarping() {
	}

	public ConfigBroxWarping(float alpha, float gamma) {
		this.alpha = alpha;
		this.gamma = gamma;
	}
}
