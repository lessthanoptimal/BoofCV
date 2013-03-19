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

package boofcv.benchmark.feature.detect;

import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.FeatureLaplacePyramid;
import boofcv.alg.feature.detect.interest.FeaturePyramid;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
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
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	List<BenchmarkAlgorithm> createAlgs(BenchmarkInterestParameters<T, D> param) {
		int radius = param.radius;
		Class<T> imageType = param.imageType;
		Class<D> derivType = param.derivType;
		int maxFeatures = param.maxFeatures;
		int maxScaleFeatures = param.maxScaleFeatures;
		double[] scales = param.scales;

		List<BenchmarkAlgorithm> ret = new ArrayList<BenchmarkAlgorithm>();

		GeneralFeatureDetector<T, D> alg;
		int thresh = 1;

		ConfigGeneralDetector configExtract = new ConfigGeneralDetector(maxFeatures,radius,thresh);

		alg = FactoryDetectPoint.createFast(
				new ConfigFast(20,9),new ConfigGeneralDetector(maxFeatures,radius,20),imageType);
		ret.add(new BenchmarkAlgorithm("Fast", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createHarris(configExtract, false, derivType);
		ret.add(new BenchmarkAlgorithm("Harris", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createShiTomasi(configExtract, false, derivType);
		ret.add(new BenchmarkAlgorithm("KLT", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createKitRos(configExtract, derivType);
		ret.add(new BenchmarkAlgorithm("KitRos", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createMedian(configExtract, imageType);
		ret.add(new BenchmarkAlgorithm("Median", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.DETERMINANT, configExtract, derivType);
		ret.add(new BenchmarkAlgorithm("Hessian", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));
		alg = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.TRACE, configExtract, derivType);
		ret.add(new BenchmarkAlgorithm("Laplace", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType)));

		FeatureLaplacePyramid<T, D> flss = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		ret.add(new BenchmarkAlgorithm("Hess Lap SS", FactoryInterestPoint.wrapDetector(flss, scales, false, imageType)));
		FeatureLaplacePyramid<T, D> flp = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		ret.add(new BenchmarkAlgorithm("Hess Lap P", FactoryInterestPoint.wrapDetector(flp, scales, true, imageType)));
		FeaturePyramid<T, D> fss = FactoryInterestPointAlgs.hessianPyramid(radius, thresh, maxScaleFeatures, imageType, derivType);
		ret.add(new BenchmarkAlgorithm("Hessian SS", FactoryInterestPoint.wrapDetector(fss, scales,false, imageType)));
		FeaturePyramid<T, D> fp = FactoryInterestPointAlgs.hessianPyramid(radius, thresh, maxScaleFeatures, imageType, derivType);
		ret.add(new BenchmarkAlgorithm("Hessian P", FactoryInterestPoint.wrapDetector(fp, scales, true,imageType)));
		ret.add(new BenchmarkAlgorithm("FastHessian", FactoryInterestPoint.<T>fastHessian(
				new ConfigFastHessian(1, 2, maxScaleFeatures, 1, 9, 4, 4))));

		return ret;
	}

}
