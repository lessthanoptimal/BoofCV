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
import boofcv.abst.feature.detdesc.WrapDetectDescribeSurf;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detdesc.DetectDescribeSift;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientation;
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

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. BoofCV provides two variants, described below.
	 * </p>
	 *
	 * <p>
	 * The modified variant provides comparable stability to binary provided by the original author.  The
	 * other variant is much faster, but a bit less stable, Both implementations contain several algorithmic
	 * changes from what was described in the original SURF paper.  See tech report [1] for details.
	 * <p>
	 *
	 * <p>
	 * Both variants use the FastHessian feature detector described in the SURF paper.
	 * </p>
	 *
	 * <p>
	 * [1] Add tech report when its finished.  See SURF performance web page for now.
	 * </p>
	 *
	 * @see FastHessianFeatureDetector
	 * @see DescribePointSurf
	 * @see DescribePointSurf
	 *
	 * @param detectThreshold       Minimum feature intensity. Image dependent.  Start tuning at 1.
	 * @param extractRadius         Radius used for non-max-suppression.  Typically 1 or 2.
	 * @param maxFeaturesPerScale   Number of features it will find or if <= 0 it will return all features it finds.
	 * @param initialSampleSize     How often pixels are sampled in the first octave.  Typically 1 or 2.
	 * @param initialSize           Typically 9.
	 * @param numberScalesPerOctave Typically 4.
	 * @param numberOfOctaves       Typically 4.
	 * @param modified              True for more stable but slower and false for a faster but less stable.
	 * @return The interest point detector.
	 * @see FastHessianFeatureDetector
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DetectDescribePoint<T,SurfFeature> surf( float detectThreshold,
											 int extractRadius, int maxFeaturesPerScale,
											 int initialSampleSize, int initialSize,
											 int numberScalesPerOctave,
											 int numberOfOctaves ,
											 boolean modified ,
											 Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral<II> orientation = FactoryOrientation.surfDefault(modified, integralType);
		DescribePointSurf<II> describe;

		if( modified ) {
			describe = FactoryDescribePointAlgs.<II>msurf(integralType);
		} else {
			describe = FactoryDescribePointAlgs.<II>surf(integralType);
		}

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(extractRadius, detectThreshold, 5, true);
		FastHessianFeatureDetector<II> detector = new FastHessianFeatureDetector<II>(extractor, maxFeaturesPerScale,
				initialSampleSize, initialSize, numberScalesPerOctave, numberOfOctaves);

		return new WrapDetectDescribeSurf<T,II>( detector, orientation, describe );
	}

	public static <T extends ImageSingleBand, D extends TupleDesc>
	DetectDescribePoint<T,D> fuseTogether( InterestPointDetector<T> detector,
										   OrientationImage<T> orientation,
										   DescribeRegionPoint<T, D> describe) {
		return new DetectDescribeFusion<T, D>(detector,orientation,describe);
	}

}
