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

package boofcv.alg.feature.detect.interest.stability;

import boofcv.alg.feature.benchmark.BenchmarkAlgorithm;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.*;
import boofcv.factory.feature.detect.interest.FactoryBlobDetector;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;


/**
 * Useful functions for benchmarking feature detection.
 *
 * @author Peter Abeles
 */
public class BenchmarkDetectHelper {
	public static <T extends ImageBase,D extends ImageBase>
	List<BenchmarkAlgorithm> createAlgs( BenchmarkInterestParameters<T,D> param )
	{
	    int radius = param.radius;
		Class<T> imageType = param.imageType;
		Class<D> derivType = param.derivType;
		int maxFeatures = param.maxFeatures;
		int maxScaleFeatures = param.maxScaleFeatures;
		double[] scales = param.scales;

		List<BenchmarkAlgorithm> ret = new ArrayList<BenchmarkAlgorithm>();

		GeneralFeatureDetector<T,D> alg;
		int thresh = 1;

		alg = FactoryCornerDetector.createFast(radius,20,maxFeatures,imageType);
		ret.add( new BenchmarkAlgorithm("Fast", FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createHarris(radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("Harris",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKlt(radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("KLT", FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKitRos(radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("KitRos",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createMedian(radius,thresh,maxFeatures,imageType);
		ret.add( new BenchmarkAlgorithm("Median",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.DETERMINANT);
		ret.add( new BenchmarkAlgorithm("Hessian",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.TRACE);
		ret.add( new BenchmarkAlgorithm("Laplace",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );

		FeatureLaplaceScaleSpace<T,D> flss = FactoryInterestPointAlgs.hessianLaplace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hess Lap SS",FactoryInterestPoint.fromFeatureLaplace(flss,scales,imageType)) );
		FeatureLaplacePyramid<T,D> flp = FactoryInterestPointAlgs.hessianLaplacePyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hess Lap P",FactoryInterestPoint.fromFeatureLaplace(flp,scales,imageType)) );
		FeatureScaleSpace<T,D> fss = FactoryInterestPointAlgs.hessianScaleSpace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hessian SS",FactoryInterestPoint.fromFeature(fss,scales,imageType)) );
		FeaturePyramid<T,D> fp = FactoryInterestPointAlgs.hessianPyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hessian P",FactoryInterestPoint.fromFeature(fp,scales,imageType)) );
		ret.add( new BenchmarkAlgorithm("FastHessian",FactoryInterestPoint.<T>fromFastHessian(maxScaleFeatures,9,4,4)) );

		return ret;
	}

}
