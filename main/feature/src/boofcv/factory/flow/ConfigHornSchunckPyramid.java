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

package boofcv.factory.flow;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.flow.HornSchunckPyramid}
 *
 * @author Peter Abeles
 */
public class ConfigHornSchunckPyramid implements Configuration {

	/**
	 * Weights importance of image brightness error and velocity smoothness.  Larger values which prefer a smooth
	 * flow.
	 */
	public float alpha = 0.05f;
	/**
	 * SOR relaxation parameter.
	 */
	public float SOR_RELAXATION = 1.9f;
	/**
	 * Number of warps which it will apply.
	 */
	public int numWarps = 10;
	/**
	 * Maximum number of iterations in the inner loop.
	 */
	public int maxInnerIterations = 150;
	/**
	 * Convergence tolerance for inner loop.  Specified in per pixel error.
	 */
	public float convergeTolerance = 1e-5f;
	/**
	 * Change in scale between each layer.  Try 0.7
	 */
	public double pyrScale = 0.7;
	/**
	 * Amount of blur applied to each layer in the pyramid.  If sigma &le; 0 then no blur is applied.
	 */
	public double pyrSigma = 0.5;
	/**
	 * Maximum number of layers in the pyramid
	 */
	public int pyrMaxLayers = 10;

	/**
	 * Type of interpolation used.  Bilinear recommended
	 */
	public InterpolationType interpolation = InterpolationType.BILINEAR;


	@Override
	public void checkValidity() {}

	public ConfigHornSchunckPyramid() {
	}

	public ConfigHornSchunckPyramid(float alpha, int maxInnerIterations) {
		this.alpha = alpha;
		this.maxInnerIterations = maxInnerIterations;
	}
}
