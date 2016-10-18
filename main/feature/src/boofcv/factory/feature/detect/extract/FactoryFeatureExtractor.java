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

package boofcv.factory.feature.detect.extract;

import boofcv.abst.feature.detect.extract.*;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.*;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.struct.image.ImageGray;

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
	 *
	 * @param intensity   Feature intensity algorithm
	 * @param extractor   Feature extraction algorithm.
	 * @param maxFeatures Maximum number of features it should return. -1 to return them all.
	 * @return General feature detector
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureDetector<I, D> general(GeneralFeatureIntensity<I, D> intensity,
										 NonMaxSuppression extractor,
										 int maxFeatures ) {
		GeneralFeatureDetector<I, D> det = new GeneralFeatureDetector<>(intensity, extractor);
		det.setMaxFeatures(maxFeatures);

		return det;
	}

	/**
	 * Standard non-max feature extractor.
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static NonMaxSuppression nonmax( ConfigExtract config ) {

		if( config == null )
			config = new ConfigExtract();
		config.checkValidity();

		NonMaxBlock ret;
		if (config.useStrictRule) {
			if( config.detectMaximums)
				if( config.detectMinimums )
					ret = new NonMaxBlockStrict.MinMax();
				else
					ret = new NonMaxBlockStrict.Max();
			else
				ret = new NonMaxBlockStrict.Min();
		} else {
			if( config.detectMaximums)
				if( config.detectMinimums )
					ret = new NonMaxBlockRelaxed.MinMax();
				else
					ret = new NonMaxBlockRelaxed.Max();
			else
				ret = new NonMaxBlockRelaxed.Min();
		}

		ret.setSearchRadius(config.radius);
		ret.setThresholdMax(config.threshold);
		ret.setThresholdMin(-config.threshold);
		ret.setBorder(config.ignoreBorder);

		return new WrapperNonMaximumBlock(ret);
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static NonMaxSuppression nonmaxCandidate(ConfigExtract config ) {

		if( config == null )
			config = new ConfigExtract();
		config.checkValidity();

		NonMaxCandidate alg;

		// no need to check the detection max/min since these algorithms can handle both
		if (config.useStrictRule) {
			alg = new NonMaxCandidateStrict();
		} else {
			alg = new NonMaxCandidateRelaxed();
		}

		WrapperNonMaxCandidate ret = new WrapperNonMaxCandidate(alg,false,true);

		ret.setSearchRadius(config.radius);
		ret.setIgnoreBorder(config.ignoreBorder);
		ret.setThresholdMaximum(config.threshold);

		return ret;
	}

	/**
	 * Creates a non-maximum limiter using the specified configuration
	 * @param config non-maxumum settings
	 * @param maxFeatures maximum allowed features
	 * @return The NonMaxLimiter
	 */
	public static NonMaxLimiter nonmaxLimiter( ConfigExtract config , int maxFeatures ) {
		NonMaxSuppression nonmax = nonmax(config);
		return new NonMaxLimiter(nonmax,maxFeatures);
	}

}
