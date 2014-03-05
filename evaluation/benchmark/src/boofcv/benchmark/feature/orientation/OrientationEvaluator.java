/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationGradient;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.distort.StabilityEvaluatorPoint;
import boofcv.evaluation.ErrorStatistics;
import boofcv.struct.image.ImageSingleBand;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class OrientationEvaluator <T extends ImageSingleBand,D extends ImageSingleBand>
		extends StabilityEvaluatorPoint<T> {

	ImageGradient<T,D> gradient;
	double angles[];
	D derivX;
	D derivY;

	ErrorStatistics errors = new ErrorStatistics(500);

	public OrientationEvaluator(int borderSize,
								InterestPointDetector<T> detector ,
								ImageGradient<T,D> gradient )
	{
		super(borderSize, detector);
		this.gradient = gradient;
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image, List<Point2D_F64> points) {
		if( derivX == null ) {
			derivX = gradient.getDerivType().createImage(image.width, image.height);
			derivY = gradient.getDerivType().createImage(image.width, image.height);
		} else {
			derivX.reshape(image.width,image.height);
			derivY.reshape(image.width,image.height);
		}

		gradient.process(image,derivX,derivY);
		RegionOrientation angleAlg = setupAlgorithm(alg, image);
		angleAlg.setScale(1);

		angles = new double[points.size()];
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 p = points.get(i);
			angles[i] = angleAlg.compute(p.x,p.y);
		}

//		ShowImages.showWindow((ImageFloat32)image,"Original",true);
	}

	private RegionOrientation setupAlgorithm(BenchmarkAlgorithm alg, T image) {
		RegionOrientation angleAlg = alg.getAlgorithm();

		if( angleAlg instanceof OrientationGradient) {
			((OrientationGradient)angleAlg).setImage(derivX,derivY);
		} else if( angleAlg instanceof OrientationImage) {
			((OrientationImage)angleAlg).setImage(image);
		} else {
			throw new IllegalArgumentException("Unknown type");
		}
		return angleAlg;
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image,  double scale , double theta,
							   List<Point2D_F64> points, List<Integer> indexes ) {

//		initToImage = initToImage.invert(null);
//		ShowImages.showWindow((ImageFloat32)image,"Modified",true);
		gradient.process(image,derivX,derivY);
		RegionOrientation angleAlg = setupAlgorithm(alg, image);

		angleAlg.setScale(scale);

		errors.reset();
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 p = points.get(i);

			double expectedAngle = UtilAngle.bound(angles[indexes.get(i)]+theta);

			double foundAngle = angleAlg.compute(p.x,p.y);
			double error = UtilAngle.dist(expectedAngle,foundAngle);
			errors.add(error);
		}

		double p50 = errors.getFraction(0.5);
		double p90 = errors.getFraction(0.9);

		return new double[]{p50,p90};
	}

	@Override
	public String[] getMetricNames() {
		return new String[]{"50% error","90% error"};
	}
}
