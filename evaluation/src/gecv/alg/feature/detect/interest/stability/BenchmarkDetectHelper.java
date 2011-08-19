/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.detect.interest.stability;

import gecv.abst.detect.interest.FactoryInterestPoint;
import gecv.alg.feature.StabilityAlgorithm;
import gecv.alg.feature.detect.intensity.HessianBlobIntensity;
import gecv.alg.feature.detect.interest.*;
import gecv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;


/**
 * Useful functions for benchmarking feature detection.
 *
 * @author Peter Abeles
 */
public class BenchmarkDetectHelper {
	public static <T extends ImageBase,D extends ImageBase>
	List<StabilityAlgorithm> createAlgs( BenchmarkInterestParameters<T,D> param )
	{
	    int radius = param.radius;
		Class<T> imageType = param.imageType;
		Class<D> derivType = param.derivType;
		int maxFeatures = param.maxFeatures;
		int maxScaleFeatures = param.maxScaleFeatures;
		double[] scales = param.scales;

		List<StabilityAlgorithm> ret = new ArrayList<StabilityAlgorithm>();

		GeneralFeatureDetector<T,D> alg;
		int thresh = 1;

		alg = FactoryCornerDetector.createFast(radius,20,maxFeatures,imageType);
		ret.add( new StabilityAlgorithm("Fast", FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createHarris(radius,thresh,maxFeatures,derivType);
		ret.add( new StabilityAlgorithm("Harris",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKlt(radius,thresh,maxFeatures,derivType);
		ret.add( new StabilityAlgorithm("KLT",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKitRos(radius,thresh,maxFeatures,derivType);
		ret.add( new StabilityAlgorithm("KitRos",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createMedian(radius,thresh,maxFeatures,imageType);
		ret.add( new StabilityAlgorithm("Median",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.DETERMINANT);
		ret.add( new StabilityAlgorithm("Hessian",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.TRACE);
		ret.add( new StabilityAlgorithm("Laplace",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );

		FeatureLaplaceScaleSpace<T,D> flss = FactoryInterestPointAlgs.hessianLaplace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new StabilityAlgorithm("Hess Lap SS",FactoryInterestPoint.fromFeatureLaplace(flss,scales,imageType)) );
		FeatureLaplacePyramid<T,D> flp = FactoryInterestPointAlgs.hessianLaplacePyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new StabilityAlgorithm("Hess Lap P",FactoryInterestPoint.fromFeatureLaplace(flp,scales,imageType)) );
		FeatureScaleSpace<T,D> fss = FactoryInterestPointAlgs.hessianScaleSpace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new StabilityAlgorithm("Hessian SS",FactoryInterestPoint.fromFeature(fss,scales,imageType)) );
		FeaturePyramid<T,D> fp = FactoryInterestPointAlgs.hessianPyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new StabilityAlgorithm("Hessian P",FactoryInterestPoint.fromFeature(fp,scales,imageType)) );
		ret.add( new StabilityAlgorithm("FastHessian",FactoryInterestPoint.<T>fromFastHessian(maxScaleFeatures,9,4,4)) );

		return ret;
	}

}
