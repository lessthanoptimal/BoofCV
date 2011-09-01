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

package gecv.alg.feature.orientation.stability;

import gecv.abst.feature.detect.interest.InterestPointDetector;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.feature.benchmark.StabilityAlgorithm;
import gecv.alg.feature.benchmark.StabilityEvaluatorPoint;
import gecv.alg.feature.orientation.OrientationGradient;
import gecv.alg.feature.orientation.OrientationImage;
import gecv.alg.feature.orientation.RegionOrientation;
import gecv.core.image.GeneralizedImageOps;
import gecv.evaluation.ErrorStatistics;
import gecv.struct.image.ImageBase;
import jgrl.metric.UtilAngle;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.point.Point2D_I32;
import jgrl.struct.point.Vector2D_F32;
import jgrl.transform.affine.AffinePointOps;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class OrientationEvaluator <T extends ImageBase,D extends ImageBase>
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
	public void extractInitial(StabilityAlgorithm alg, T image, List<Point2D_I32> points) {
		if( derivX == null ) {
			derivX = GeneralizedImageOps.createImage(gradient.getDerivType(),image.width,image.height);
			derivY = GeneralizedImageOps.createImage(gradient.getDerivType(),image.width,image.height);
		} else {
			derivX.reshape(image.width,image.height);
			derivY.reshape(image.width,image.height);
		}

		gradient.process(image,derivX,derivY);
		RegionOrientation angleAlg = setupAlgorithm(alg, image);
		angleAlg.setScale(1);

		angles = new double[points.size()];
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);
			angles[i] = angleAlg.compute(p.x,p.y);
		}

//		ShowImages.showWindow((ImageFloat32)image,"Original",true);
	}

	private RegionOrientation setupAlgorithm(StabilityAlgorithm alg, T image) {
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
	public double[] evaluateImage(StabilityAlgorithm alg, T image, Affine2D_F32 initToImage,
							   List<Point2D_I32> points, List<Integer> indexes ) {

//		initToImage = initToImage.invert(null);
//		ShowImages.showWindow((ImageFloat32)image,"Modified",true);
		gradient.process(image,derivX,derivY);
		RegionOrientation angleAlg = setupAlgorithm(alg, image);

		Vector2D_F32 v1 = new Vector2D_F32();
		Vector2D_F32 v2 = new Vector2D_F32();

		// assume that the feature's scale was correctly estimated by the detection algorithm
		if( initToImage != null ) {
			v1.x = 1;
			v1.y = 1;
			AffinePointOps.transform(initToImage,v1,v2);
			double scale = v2.norm()/v1.norm();
//			System.out.println("Scale "+scale+"  "+points.size());
			angleAlg.setScale(scale);
		} else {
			angleAlg.setScale(1.0);
		}

		errors.reset();
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);

			double expectedAngle;

			if( initToImage != null ) {
				int index = indexes.get(i);
				// find the direction in this frame by applying the transform
				v1.x = (float)Math.cos(angles[index]);
				v1.y = (float)Math.sin(angles[index]);
				AffinePointOps.transform(initToImage,v1,v2);
				expectedAngle = Math.atan2(v2.y,v2.x);
			} else {
				expectedAngle = angles[i];
			}

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
