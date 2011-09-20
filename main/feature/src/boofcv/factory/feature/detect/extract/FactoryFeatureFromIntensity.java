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
import boofcv.alg.feature.detect.extract.NonMaxCandidateExtractor;

/**
 * Given a list of requirements create a {@link boofcv.abst.feature.detect.extract.FeatureExtractor} that meets
 * those requirements.
 *
 * @author Peter Abeles
 */
public class FactoryFeatureFromIntensity
{

	/**
	 * Creates a corner extractor with the following properties.  It is assumed that any detector returned will
	 * be of the non-max suppression variety.  
	 *
	 * @param useCandidateList Will it use the provided list of candidate features?
	 * @param excludeCorners Can it exclude all corners in a list?
	 * @param ignoreBorderIntensity How many pixels in the intensity image were not processed.
	 * @param acceptRequestNumber Will it detect features until the specified number have been found?
	 * @return A feature extractor.
	 */
	public static FeatureExtractor create( int minSeparation , float threshold , int ignoreBorderIntensity ,
										  boolean useCandidateList , boolean excludeCorners , boolean acceptRequestNumber )
	{
		FeatureExtractor ret = null;

		if( useCandidateList ) {
			if( !acceptRequestNumber && ignoreBorderIntensity <= minSeparation)
				ret = new WrapperNonMaxCandidate(new NonMaxCandidateExtractor(minSeparation,ignoreBorderIntensity,threshold));
		} else {
			if( !acceptRequestNumber ) {
				ret = new WrapperNonMax(new FastNonMaxExtractor(minSeparation,ignoreBorderIntensity,threshold));
			} else {
				throw new IllegalArgumentException("Need to create a wrapper for SelectNBestFeatures");
			}
		}

		if(ret == null )
			throw new IllegalArgumentException("No filter exists which matches the specified requirements");

		if( useCandidateList && !ret.getUsesCandidates() )
			throw new RuntimeException("BUG: Returned algorithm does support candidate list");
		if( excludeCorners && !ret.getCanExclude() )
			throw new RuntimeException("BUG: Returned algorithm does support exclude corners");
		if( acceptRequestNumber && !ret.getAcceptRequest() )
			throw new RuntimeException("BUG: Returned algorithm does support request number");

		return ret;
	}
}
