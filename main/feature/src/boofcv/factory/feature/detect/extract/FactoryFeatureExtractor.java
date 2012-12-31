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

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.WrapperNonMaxCandidate;
import boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.extract.*;
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
	 * @param intensity   Feature intensity algorithm
	 * @param extractor   Feature extraction algorithm.
	 * @param maxFeatures Maximum number of features it should return. -1 to return them all.
	 * @param <I>         Input image type.
	 * @param <D>         Image derivative type.
	 * @return General feature detector
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<I, D> general(GeneralFeatureIntensity<I, D> intensity,
										 FeatureExtractor extractor,
										 int maxFeatures) {
		GeneralFeatureDetector<I, D> det = new GeneralFeatureDetector<I, D>(intensity, extractor);
		det.setMaxFeatures(maxFeatures);

		return det;
	}

	/**
	 * Standard non-max feature extractor.
	 *
	 * @param searchRadius  Radius of the non-maximum region.
	 * @param threshold     Minimum feature intensity it will consider
	 * @param ignoreBorder  Size of border around the image in which pixels are not considered.
	 * @param useStrictRule Is a strict test used to test for local maximums.
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmax(int searchRadius,
										  float threshold,
										  int ignoreBorder,
										  boolean useStrictRule) {

		NonMaxBlock ret;
		if (useStrictRule) {
			ret = new NonMaxBlockStrict();
		} else {
			ret = new NonMaxBlockRelaxed();
		}

		ret.setSearchRadius(searchRadius);
		ret.setThreshold(threshold);
		ret.setBorder(ignoreBorder);

		return new WrapperNonMaximumBlock(ret);
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param searchRadius  Minimum separation between found features.
	 * @param threshold     Minimum feature intensity it will consider
	 * @param ignoreBorder  Size of border around the image in which pixels are not considered.
	 * @param useStrictRule Is a strict test used to test for local maximums.
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmaxCandidate(int searchRadius, float threshold,
												   int ignoreBorder, boolean useStrictRule) {
		WrapperNonMaxCandidate ret;

		if (useStrictRule)
			ret = new WrapperNonMaxCandidate(new NonMaxCandidateStrict(searchRadius, threshold, ignoreBorder));
		else
			ret = new WrapperNonMaxCandidate(new NonMaxCandidateRelaxed(searchRadius, threshold, ignoreBorder));

		return ret;
	}

}
