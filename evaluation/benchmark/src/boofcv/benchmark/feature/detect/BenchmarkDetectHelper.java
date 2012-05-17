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

package boofcv.benchmark.feature.detect;

import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.FeatureLaplacePyramid;
import boofcv.alg.feature.detect.interest.FeatureLaplaceScaleSpace;
import boofcv.alg.feature.detect.interest.FeaturePyramid;
import boofcv.alg.feature.detect.interest.FeatureScaleSpace;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.image.ImageSingleBand;

import java.util.ArrayList;
import java.util.List;


/**
 * Useful functions for benchmarking feature detection.
 *
 * @author Peter Abeles
 */
public class BenchmarkDetectHelper {
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
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
		ret.add( new BenchmarkAlgorithm("Fast", FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createHarris(radius, false, thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("Harris",FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createShiTomasi(radius, false, thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("KLT", FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createKitRos(radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("KitRos",FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createMedian(radius,thresh,maxFeatures,imageType);
		ret.add( new BenchmarkAlgorithm("Median",FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createHessian(HessianBlobIntensity.Type.DETERMINANT,radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("Hessian",FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );
		alg = FactoryCornerDetector.createHessian(HessianBlobIntensity.Type.TRACE,radius,thresh,maxFeatures,derivType);
		ret.add( new BenchmarkAlgorithm("Laplace",FactoryInterestPoint.wrapCorner(alg, imageType, derivType)) );

		FeatureLaplaceScaleSpace<T,D> flss = FactoryInterestPointAlgs.hessianLaplace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hess Lap SS",FactoryInterestPoint.wrapDetector(flss, scales, imageType)) );
		FeatureLaplacePyramid<T,D> flp = FactoryInterestPointAlgs.hessianLaplacePyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hess Lap P",FactoryInterestPoint.wrapDetector(flp, scales, imageType)) );
		FeatureScaleSpace<T,D> fss = FactoryInterestPointAlgs.hessianScaleSpace(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hessian SS",FactoryInterestPoint.wrapDetector(fss, scales, imageType)) );
		FeaturePyramid<T,D> fp = FactoryInterestPointAlgs.hessianPyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		ret.add( new BenchmarkAlgorithm("Hessian P",FactoryInterestPoint.wrapDetector(fp, scales, imageType)) );
		ret.add( new BenchmarkAlgorithm("FastHessian",FactoryInterestPoint.<T>fastHessian(1, 2, maxScaleFeatures, 1, 9, 4, 4)) );

		return ret;
	}

}
