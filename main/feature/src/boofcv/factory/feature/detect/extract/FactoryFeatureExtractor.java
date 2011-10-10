/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.abst.feature.detect.extract.WrapperNonMax;
import boofcv.abst.feature.detect.extract.WrapperNonMaxCandidate;
import boofcv.alg.feature.detect.extract.FastNonMaxExtractor;
import boofcv.alg.feature.detect.extract.NonMaxBorderExtractor;
import boofcv.alg.feature.detect.extract.NonMaxCandidateExtractor;

/**
 * Given a list of requirements create a {@link boofcv.abst.feature.detect.extract.FeatureExtractor} that meets
 * those requirements.
 *
 * @author Peter Abeles
 */
public class FactoryFeatureExtractor
{
	/**
	 * Standard non-max feature extractor.
	 *
	 *
	 * @param minSeparation Minimum separation between found features.
	 * @param threshold Minimum feature intensity it will consider
	 * @param ignoreBorderIntensity How many pixels in the intensity image were not processed.
	 * @param detectBorderFeatures Should it detect feature's whose region intersect the image border?
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmax(int minSeparation,
										  float threshold,
										  int ignoreBorderIntensity, boolean detectBorderFeatures) {

		NonMaxBorderExtractor extractorBorder = null;
		if( detectBorderFeatures ) {
			extractorBorder = new NonMaxBorderExtractor(minSeparation,threshold);
		}

		WrapperNonMax ret = new WrapperNonMax(new FastNonMaxExtractor(minSeparation,threshold), extractorBorder);
		ret.setInputBorder(ignoreBorderIntensity);
		return ret;
	}

	/**
	 * Non-max feature extractor which saves a candidate list of all the found local maximums..
	 *
	 * @param minSeparation Minimum separation between found features.
	 * @param threshold Minimum feature intensity it will consider
	 * @param ignoreBorderIntensity How many pixels in the intensity image were not processed.
	 * @return A feature extractor.
	 */
	public static FeatureExtractor nonmaxCandidate( int minSeparation , float threshold , int ignoreBorderIntensity ) {
		WrapperNonMaxCandidate ret = new WrapperNonMaxCandidate(new NonMaxCandidateExtractor(minSeparation,threshold));
		ret.setInputBorder(ignoreBorderIntensity);
		return ret;
	}

}
