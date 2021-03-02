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
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.orientation.ConfigSiftOrientation;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import org.jetbrains.annotations.Nullable;

/**
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

		SiftScaleSpace scaleSpace = new SiftScaleSpace(
				configSS.firstOctave, configSS.lastOctave, configSS.numScales, configSS.sigma0);
		OrientationHistogramSift<GrayF32> orientation = new OrientationHistogramSift<>(
				configOri.histogramSize, configOri.sigmaEnlarge, GrayF32.class);
		DescribePointSift<GrayF32> describe = new DescribePointSift<>(
				configDesc.widthSubregion, configDesc.widthGrid, configDesc.numHistogramBins,
				configDesc.sigmaToPixels, configDesc.weightingSigmaFraction,
				configDesc.maxDescriptorElementValue, GrayF32.class);

		NonMaxLimiter nonMax = FactoryFeatureExtractor.nonmaxLimiter(
				configDetector.extract, configDetector.selector, configDetector.maxFeaturesPerScale);
		FeatureSelectLimitIntensity<ScalePoint> selectorAll = FactorySelectLimit.intensity(configDetector.selector);
		CompleteSift dds = new CompleteSift(scaleSpace, selectorAll,
				configDetector.edgeR, nonMax, orientation, describe);
		dds.maxFeaturesAll = configDetector.maxFeaturesAll;
		return dds;
	}
}
