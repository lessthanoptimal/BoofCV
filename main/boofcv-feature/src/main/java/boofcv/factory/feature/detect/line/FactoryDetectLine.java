/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.line;


import boofcv.abst.feature.detect.line.*;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.image.ImageGray;

import javax.annotation.Nullable;

/**
 * Factory for creating high level implementations of {@link DetectLine} and {@Link DetectLineSegment}. For more access
 * to internal data structures see {@link FactoryDetectLineAlgs}.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLine {

	/**
	 * Detects line segments inside an image using the {@link DetectLineSegmentsGridRansac} algorithm.
	 *
	 * @see DetectLineSegmentsGridRansac
	 *
	 * @param config Configuration for line detector
	 * @param imageType Type of single band input image.
	 * @return Line segment detector
	 */
	public static <I extends ImageGray<I>>
	DetectLineSegment<I> lineRansac( @Nullable ConfigLineRansac config,
									 Class<I> imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		return FactoryDetectLineAlgs.lineRansac(config,imageType,derivType);
	}

	/**
	 * Detects lines using the foot of norm parametrization, see {@link DetectLineHoughFoot}.  The polar
	 * parametrization is more common, but more difficult to tune.
	 *
	 * @see DetectLineHoughFoot
	 *
	 * @param config Configuration for line detector.  If null then default will be used.
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghFoot(@Nullable ConfigHoughFoot config , Class<T> imageType ) {
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DetectEdgeLines alg = FactoryDetectLineAlgs.houghFoot(config,derivType);

		return new DetectEdgeLinesToLines(alg,imageType,derivType);
	}

	/**
	 * Detects lines using a foot of norm parametrization and sub images to reduce degenerate
	 * configurations, see {@link DetectLineHoughFootSubimage} for details.
	 *
	 * @see DetectLineHoughFootSubimage
	 *
	 * @param config Configuration for line detector.  If null then default will be used.
	 * @param imageType input image type
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghFootSub(@Nullable ConfigHoughFootSubimage config , Class<T> imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DetectEdgeLines alg = FactoryDetectLineAlgs.houghFootSub(config,derivType);

		return new DetectEdgeLinesToLines(alg,imageType,derivType);
	}

	/**
	 * Creates a Hough line detector based on polar parametrization. Finds lines along the edges of objects using
	 * the image gradient.
	 *
	 * @see DetectLineHoughPolarEdge
	 *
	 * @param config Configuration for line detector.  Can't be null.
	 * @param imageType input image type
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghPolar(ConfigHoughPolar config , Class<T> imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DetectEdgeLines alg = FactoryDetectLineAlgs.houghPolarEdge(config,derivType);

		return new DetectEdgeLinesToLines(alg,imageType,derivType);
	}

	/**
	 * Creates a Hough line detector based on polar parametrization. Input image is converted into a binary image.
	 * Every 1 pixel in binary image is assumed to belong to a line.
	 *
	 * @see DetectLineHoughPolarBinary
	 *
	 * @param configHough Configuration for line detector.  Can't be null.
	 * @param configThreshold Configuration for threshold algorithm. Can be null. Defaults to OTSU
	 * @param imageType input image type
	 * @return Line detector.
	 */
	public static <T extends ImageGray<T>>
	DetectLine<T> houghPolar(ConfigHoughPolar configHough , @Nullable ConfigThreshold configThreshold ,
							 Class<T> imageType )
	{
		if( configThreshold == null ) {
			configThreshold = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);
		}

		InputToBinary<T> binarization = FactoryThresholdBinary.threshold(configThreshold,imageType);
		DetectLineHoughPolarBinary alg = FactoryDetectLineAlgs.houghPolarBinary(configHough);

		return new DetectBinaryLinesToLines<>(alg,binarization);
	}

}
