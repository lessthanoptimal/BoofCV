/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.extract.WrapperNonMax;
import boofcv.abst.feature.detect.extract.WrapperNonMaxCandidate;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.FastNonMaxExtractor;
import boofcv.alg.feature.detect.extract.NonMaxBorderExtractor;
import boofcv.alg.feature.detect.extract.NonMaxCandidateRelaxed;
import boofcv.alg.feature.detect.extract.NonMaxCandidateStrict;
import boofcv.struct.image.ImageSingleBand;

/**
 * Creates {@link FeatureExtractor} for finding local maximums in feature intensity images.
 *
 * @see boofcv.factory.feature.detect.intensity.FactoryIntensityPoint
 *
 * @author Peter Abeles
 */
public class FactoryFeatureExtractor
{
	/**
	 * Creates a generalized feature detector/extractor that adds n-best capability to {@link FeatureExtractor}
	 * and performs other house keeping tasks. Handles calling {@link GeneralFeatureIntensity} itself.
	 *
	 * @param intensity Feature intensity algorithm
	 * @param extractor Feature extraction algorithm.
	 * @param maxFeatures Maximum number of features it should return. -1 to return them all.
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return General feature detector
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<I,D> general( GeneralFeatureIntensity<I, D> intensity,
										 FeatureExtractor extractor,
										 int maxFeatures) {
		return new GeneralFeatureDetector<I, D>(intensity,extractor,maxFeatures);
	}
	
	
	
	
	/**
	 * Standard non-max feature extractor.
	 *
	 * @param minSeparation Minimum separation between found features.
	 * @param threshold Minimum feature intensity it will consider
	 * @param ignoreBorderIntensity How many pixels in the intensity image were not processed.
	 * @param detectBorderFeatures Should it detect feature's whose region intersect the image border?
	 * @param useStrictRule Is a strict test used to test for local maximums.
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmax(int minSeparation,
										  float threshold,
										  int ignoreBorderIntensity, boolean detectBorderFeatures,
										  boolean useStrictRule) {

		NonMaxBorderExtractor extractorBorder = null;
		if( detectBorderFeatures ) {
			extractorBorder = new NonMaxBorderExtractor(minSeparation,threshold,useStrictRule);
		}

		WrapperNonMax ret = new WrapperNonMax(new FastNonMaxExtractor(minSeparation,threshold,useStrictRule), extractorBorder);
		ret.setInputBorder(ignoreBorderIntensity);
		return ret;
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param minSeparation Minimum separation between found features.
	 * @param threshold Minimum feature intensity it will consider
	 * @param ignoreBorderIntensity How many pixels in the intensity image were not processed.
	 * @param detectBorderExtractor Should it detect feature's whose region intersect the image border?
	 * @param useStrictRule Is a strict test used to test for local maximums.
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmaxCandidate(int minSeparation, float threshold,
												   int ignoreBorderIntensity,
												   boolean detectBorderExtractor, boolean useStrictRule) {
		WrapperNonMaxCandidate ret;

		if( useStrictRule )
			ret = new WrapperNonMaxCandidate(new NonMaxCandidateStrict(minSeparation,threshold, detectBorderExtractor));
		else
			ret = new WrapperNonMaxCandidate(new NonMaxCandidateRelaxed(minSeparation,threshold, detectBorderExtractor));

		ret.setInputBorder(ignoreBorderIntensity);
		return ret;
	}

}
