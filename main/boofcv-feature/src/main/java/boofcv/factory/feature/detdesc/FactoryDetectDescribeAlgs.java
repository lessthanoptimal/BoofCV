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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detdesc.ConfigCompleteSift;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.orientation.ConfigSiftOrientation;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.alg.feature.detdesc.CompleteSift_MT;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.GrayF32;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for specific implementations of Detect and Describe feature algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryDetectDescribeAlgs {
	public static CompleteSift sift( @Nullable ConfigCompleteSift config ) {
		if (config == null)
			config = new ConfigCompleteSift();

		ConfigSiftScaleSpace configSS = config.scaleSpace;
		ConfigSiftDetector configDetector = config.detector;
		ConfigSiftOrientation configOri = config.orientation;
		ConfigSiftDescribe configDesc = config.describe;

		var ss = new SiftScaleSpace(configSS.firstOctave, configSS.lastOctave, configSS.numScales, configSS.sigma0);
		SiftDetector detector = FactoryInterestPointAlgs.sift(configDetector);
		detector.maxFeaturesAll = configDetector.maxFeaturesAll;

		// Create the threaded variant if requested
		if (BoofConcurrency.USE_CONCURRENT) {
			return new CompleteSift_MT(ss, detector,
					() -> new OrientationHistogramSift<>(
							configOri.histogramSize, configOri.sigmaEnlarge, GrayF32.class),
					() -> new DescribePointSift<>(
							configDesc.widthSubregion, configDesc.widthGrid, configDesc.numHistogramBins,
							configDesc.sigmaToPixels, configDesc.weightingSigmaFraction,
							configDesc.maxDescriptorElementValue, GrayF32.class));
		} else {
			OrientationHistogramSift<GrayF32> orientation = new OrientationHistogramSift<>(
					configOri.histogramSize, configOri.sigmaEnlarge, GrayF32.class);

			DescribePointSift<GrayF32> describe = new DescribePointSift<>(
					configDesc.widthSubregion, configDesc.widthGrid, configDesc.numHistogramBins,
					configDesc.sigmaToPixels, configDesc.weightingSigmaFraction,
					configDesc.maxDescriptorElementValue, GrayF32.class);

			return new CompleteSift(ss, detector, orientation, describe);
		}
	}
}
