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

package boofcv.benchmark.feature.orientation;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.ImageSingleBand;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class UtilOrientationBenchmark {

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> defaultDetector( Class<T> imageType , Class<D> derivType ) {
		return FactoryInterestPoint.<T>fastHessian(new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4));
//		GeneralFeatureDetector<T, D> detector = FactoryDetectPoint.createKlt(2,0.1f,150,derivType);
//		FeatureScaleSpace<T,D> ff = new FeatureScaleSpace<T,D>(detector,2);
//		double scales[] = new double[]{1,1.2,1.5,3,4,5,6,7};
//		return FactoryInterestPoint.wrapDetector(ff,scales,imageType);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	List<BenchmarkAlgorithm> createAlgorithms( int radius , Class<T> imageType , Class<D> derivType )
	{
		List<BenchmarkAlgorithm> ret = new ArrayList<BenchmarkAlgorithm>();

		ret.add(new BenchmarkAlgorithm("Ave Unweighted", FactoryOrientationAlgs.average(radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Ave Weighted", FactoryOrientationAlgs.average(radius,true,derivType)));
		ret.add(new BenchmarkAlgorithm("Hist5 Unweighted", FactoryOrientationAlgs.histogram(5,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Hist5 Weighted", FactoryOrientationAlgs.histogram(5,radius,true,derivType)));
		ret.add(new BenchmarkAlgorithm("Hist10 Unweighted", FactoryOrientationAlgs.histogram(10,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Hist10 Weighted", FactoryOrientationAlgs.histogram(10,radius,true,derivType)));
		ret.add(new BenchmarkAlgorithm("Hist20 Unweighted", FactoryOrientationAlgs.histogram(20,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Slide PI/6 Un-W", FactoryOrientationAlgs.sliding(10,Math.PI/6,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Slide PI/6 W", FactoryOrientationAlgs.sliding(10,Math.PI/6,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Slide PI/3 Un-W", FactoryOrientationAlgs.sliding(20,Math.PI/3,radius,false,derivType)));
		ret.add(new BenchmarkAlgorithm("Slide PI/3 W", FactoryOrientationAlgs.sliding(20,Math.PI/3,radius,true,derivType)));
		ret.add(new BenchmarkAlgorithm("No Gradient", FactoryOrientationAlgs.nogradient(radius,imageType)));

		Class typeII = GIntegralImageOps.getIntegralType(imageType);
		ret.add(new BenchmarkAlgorithm("II Ave",
				new WrapII(FactoryOrientationAlgs.average_ii(new ConfigAverageIntegral(radius,1,4,0), typeII),imageType)));
		ret.add(new BenchmarkAlgorithm("II Ave Weighted",
				new WrapII(FactoryOrientationAlgs.average_ii(new ConfigAverageIntegral(radius, 1,4,-1), typeII),imageType)));

		return ret;
	}

	private static class WrapII<T extends ImageSingleBand, II extends ImageSingleBand> implements  OrientationImage<T> {

		OrientationIntegral<II> alg;
		II ii;
		Class<T> imageType;

		private WrapII(OrientationIntegral<II> alg , Class<T> imageType ) {
			this.alg = alg;
			this.imageType = imageType;
			ii = GeneralizedImageOps.createSingleBand(alg.getImageType(), 1, 1);
		}

		@Override
		public void setImage(T image) {
			ii.reshape(image.width,image.height);
			GIntegralImageOps.transform(image,ii);
			alg.setImage(ii);
		}

		@Override
		public Class<T> getImageType() {
			return imageType;
		}

		@Override
		public void setScale(double scale) {
			alg.setScale(scale);
		}

		@Override
		public double compute(double c_x, double c_y) {
			return alg.compute(c_x,c_y);
		}
	}

	public static double[] makeSample( double min , double max , int num ) {
		double []ret = new double[ num ];
		for( int i = 0; i < num; i++ ) {
			ret[i]= min + i*(max-min)/(num-1);
		}
		return ret;
	}

}
