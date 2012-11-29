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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detdesc.WrapDetectDescribeSift;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detdesc.DetectDescribeSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.feature.orientation.OrientationImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
public class FactoryDetectDescribe {

	public static DetectDescribePoint<ImageFloat32,SurfFeature>
	sift( int numOfOctaves ,
		  boolean doubleInputImage ,
		  int maxFeaturesPerScale ) {
		return sift(1.6,5,numOfOctaves,doubleInputImage,3,0,maxFeaturesPerScale,10,36);
	}

	public static DetectDescribePoint<ImageFloat32,SurfFeature>
	sift( double scaleSigma ,
		  int numOfScales ,
		  int numOfOctaves ,
		  boolean doubleInputImage ,
		  int extractRadius,
		  float detectThreshold,
		  int maxFeaturesPerScale,
		  double edgeThreshold ,
		  int oriHistogramSize ) {

		double sigmaToRadius = BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;

		SiftImageScaleSpace ss = new SiftImageScaleSpace((float)scaleSigma, numOfScales, numOfOctaves,
				doubleInputImage);

		SiftDetector detector = FactoryInterestPointAlgs.siftDetector(extractRadius, detectThreshold,
				maxFeaturesPerScale, edgeThreshold);

		OrientationHistogramSift orientation = new OrientationHistogramSift(oriHistogramSize,sigmaToRadius,1.5);
		DescribePointSift describe = new DescribePointSift(4,8,8,0.5, sigmaToRadius);

		DetectDescribeSift combined = new DetectDescribeSift(ss,detector,orientation,describe);

		return new WrapDetectDescribeSift(combined);
	}

	public static <T extends ImageSingleBand, D extends TupleDesc>
	DetectDescribePoint<T,D> fuseTogether( InterestPointDetector<T> detector,
										   OrientationImage<T> orientation,
										   DescribeRegionPoint<T, D> describe) {
		return new DetectDescribeFusion<T, D>(detector,orientation,describe);
	}

}
