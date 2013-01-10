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

package boofcv.factory.feature.detect.extract;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.WrapperNonMaxCandidate;
import boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.*;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.struct.image.ImageSingleBand;

/**
 * Creates {@link FeatureExtractor} for finding local maximums in feature intensity images.
 *
 * @author Peter Abeles
 * @see boofcv.factory.feature.detect.intensity.FactoryIntensityPoint
 */
public class FactoryFeatureExtractor {
	/**
	 * Creates a generalized feature detector/extractor that adds n-best capability to {@link FeatureExtractor}
	 * and performs other house keeping tasks. Handles calling {@link GeneralFeatureIntensity} itself.
	 *
	 *
	 * @param intensity   Feature intensity algorithm
	 * @param extractor   Feature extraction algorithm.
	 * @param maxFeatures Maximum number of features it should return. -1 to return them all.
	 * @param detectMinimum if true it will detect local minimums as well as maximums.
	 * @return General feature detector
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<I, D> general(GeneralFeatureIntensity<I, D> intensity,
										 FeatureExtractor extractor,
										 int maxFeatures, boolean detectMinimum) {
		GeneralFeatureDetector<I, D> det = new GeneralFeatureDetector<I, D>(intensity, extractor,detectMinimum);
		det.setMaxFeatures(maxFeatures);

		return det;
	}

	/**
	 * Standard non-max feature extractor.
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmax( ConfigExtract config ) {

		if( config == null )
			config = new ConfigExtract();
		config.checkValidity();

		NonMaxBlock ret;
		if (config.useStrictRule) {
			ret = new NonMaxBlockStrict();
		} else {
			ret = new NonMaxBlockRelaxed();
		}

		ret.setSearchRadius(config.radius);
		ret.setThreshold(config.threshold);
		ret.setBorder(config.ignoreBorder);

		return new WrapperNonMaximumBlock(ret,false,true);
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param config Configuration for extractor
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmaxCandidate(ConfigExtract config ) {

		if( config == null )
			config = new ConfigExtract();
		config.checkValidity();

		WrapperNonMaxCandidate ret;

		if (config.useStrictRule)
			ret = new WrapperNonMaxCandidate(
					new NonMaxCandidateStrict());
		else
			ret = new WrapperNonMaxCandidate(
					new NonMaxCandidateRelaxed());

		ret.setSearchRadius(config.radius);
		ret.setIgnoreBorder(config.ignoreBorder);
		ret.setThreshold(config.threshold);


		return ret;
	}

}
