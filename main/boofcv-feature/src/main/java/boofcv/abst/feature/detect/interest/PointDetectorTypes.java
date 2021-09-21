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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.intensity.MedianCornerIntensity;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;

/**
 * List of all the built in point detectors
 *
 * @author Peter Abeles
 */
public enum PointDetectorTypes {
	/**
	 * Maximums only
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 */
	SHI_TOMASI,
	/**
	 * Maximums only
	 *
	 * @see boofcv.alg.feature.detect.intensity.HarrisCornerIntensity
	 */
	HARRIS,
	/**
	 * Maximums and minimums
	 *
	 * @see FastCornerDetector
	 */
	FAST,
	/**
	 * Computed by directly sampling input image. Maximums and minimums
	 *
	 * @see FactoryDetectPoint#createHessianDirect
	 */
	LAPLACIAN,
	/**
	 * Computed by directly sampling input image. Maximums only
	 *
	 * @see FactoryDetectPoint#createHessianDirect
	 */
	DETERMINANT,

	/** {@link boofcv.alg.feature.detect.intensity.KitRosCornerIntensity} */
	KIT_ROS,

	/** {@link MedianCornerIntensity} */
	MEDIAN,
	/**
	 * Computed using the Hessian image. Maximums and minimums
	 *
	 * @see HessianBlobIntensity
	 */
	LAPLACIAN_H,
	/**
	 * Computed using the Hessian image. Maximums only
	 *
	 * @see HessianBlobIntensity
	 */
	DETERMINANT_H;

	/**
	 * Point types which take in the input image or the gradient (first-derivative) image only.
	 */
	public static final PointDetectorTypes[] FIRST_ONLY = new PointDetectorTypes[]{SHI_TOMASI, HARRIS, FAST, LAPLACIAN, DETERMINANT, KIT_ROS, MEDIAN};
}
