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

package boofcv.benchmark.feature.distort;

import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.struct.image.ImageSingleBand;


/**
 * Evaluates how similar the output of the original image is modified version of it.
 *
 * @author Peter Abeles
 */
public interface StabilityEvaluator<T extends ImageSingleBand> {

	/**
	 * Extracts information from the original image
	 * @param alg Algorithm being evaluated.
	 * @param image Original image.
	 */
	void extractInitial( BenchmarkAlgorithm alg , T image );

	/**
	 * Extracts information from a modified image and compares
	 * to what was extracted from the original image.
	 *
	 * @param alg Algorithm being evaluated.
	 * @param image Modified image.
	 * @param param Describes how the image was distorted.
	 * @return Error metrics.
	 */
	double[] evaluateImage(BenchmarkAlgorithm alg, T image, DistortParam param );

	/**
	 * Names of extracted metrics.
	 * @return metric names.
	 */
	public abstract String[] getMetricNames();
}
