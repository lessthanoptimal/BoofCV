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

package boofcv.factory.feature.detect.extract;

import boofcv.abst.feature.detect.extract.*;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.*;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;

/**
 * Creates {@link boofcv.abst.feature.detect.extract.NonMaxSuppression} for finding local maximums in feature intensity images.
 *
 * @author Peter Abeles
 * @see boofcv.factory.feature.detect.intensity.FactoryIntensityPoint
 */
public class FactoryFeatureExtractor {
	/**
	 * Creates a generalized feature detector/extractor that adds n-best capability to {@link boofcv.abst.feature.detect.extract.NonMaxSuppression}
	 * and performs other house keeping tasks. Handles calling {@link GeneralFeatureIntensity} itself.
	 *
	 * @param intensity Feature intensity algorithm
	 * @param extractorMin Feature extraction algorithm for local minimums
	 * @param extractorMax Feature extraction algorithm for local maximums
	 * @param selector Selects features when there is more than the maximum allowed
	 * @param maxFeatures Maximum number of features it should return. -1 to return them all.
	 * @return General feature detector
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	GeneralFeatureDetector<I, D> general( GeneralFeatureIntensity<I, D> intensity,
										  @Nullable NonMaxSuppression extractorMin,
										  @Nullable NonMaxSuppression extractorMax,
										  FeatureSelectLimitIntensity<Point2D_I16> selector,
										  int maxFeatures ) {
		GeneralFeatureDetector<I, D> det = new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);
		det.setFeatureLimit(maxFeatures);

		return det;
	}

	/**
	 * Standard non-max feature extractor.
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static NonMaxSuppression nonmax( @Nullable ConfigExtract config ) {

		if (config == null)
			config = new ConfigExtract();
		config.checkValidity();

		if (BOverrideFactoryFeatureExtractor.nonmax != null) {
			return BOverrideFactoryFeatureExtractor.nonmax.process(config);
		}

		NonMaxBlock.Search search;
		if (config.useStrictRule) {
			if (config.detectMaximums)
				if (config.detectMinimums)
					search = new NonMaxBlockSearchStrict.MinMax();
				else
					search = new NonMaxBlockSearchStrict.Max();
			else
				search = new NonMaxBlockSearchStrict.Min();
		} else {
			if (config.detectMaximums)
				if (config.detectMinimums)
					search = new NonMaxBlockSearchRelaxed.MinMax();
				else
					search = new NonMaxBlockSearchRelaxed.Max();
			else
				search = new NonMaxBlockSearchRelaxed.Min();
		}

		// See if the user wants to use threaded code or not
		NonMaxBlock alg = BoofConcurrency.USE_CONCURRENT ?
				new NonMaxBlock_MT(search) : new NonMaxBlock(search);

		alg.setSearchRadius(config.radius);
		alg.setThresholdMax(config.threshold);
		alg.setThresholdMin(-config.threshold);
		alg.setBorder(config.ignoreBorder);

		return new WrapperNonMaximumBlock(alg);
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static NonMaxSuppression nonmaxCandidate( @Nullable ConfigExtract config ) {

		if (config == null)
			config = new ConfigExtract();
		config.checkValidity();

		if (BOverrideFactoryFeatureExtractor.nonmaxCandidate != null) {
			return BOverrideFactoryFeatureExtractor.nonmaxCandidate.process(config);
		}

		NonMaxCandidate.Search search;

		// no need to check the detection max/min since these algorithms can handle both
		if (config.useStrictRule) {
			search = new NonMaxCandidate.Strict();
		} else {
			search = new NonMaxCandidate.Relaxed();
		}

		// See if the user wants to use threaded code or not
		NonMaxCandidate extractor = BoofConcurrency.USE_CONCURRENT ?
				new NonMaxCandidate_MT(search) : new NonMaxCandidate(search);

		WrapperNonMaxCandidate ret = new WrapperNonMaxCandidate(extractor, false, true);

		ret.setSearchRadius(config.radius);
		ret.setIgnoreBorder(config.ignoreBorder);
		ret.setThresholdMaximum(config.threshold);

		return ret;
	}

	/**
	 * Creates a non-maximum limiter using the specified configuration
	 *
	 * @param configExtract non-maxumum settings
	 * @param maxFeatures maximum allowed features
	 * @return The NonMaxLimiter
	 */
	public static NonMaxLimiter nonmaxLimiter( @Nullable ConfigExtract configExtract, ConfigSelectLimit configSelect, int maxFeatures ) {
		NonMaxSuppression nonmax = nonmax(configExtract);
		FeatureSelectLimitIntensity<NonMaxLimiter.LocalExtreme> selector = FactorySelectLimit.intensity(configSelect);
		return new NonMaxLimiter(nonmax, selector, maxFeatures);
	}
}
