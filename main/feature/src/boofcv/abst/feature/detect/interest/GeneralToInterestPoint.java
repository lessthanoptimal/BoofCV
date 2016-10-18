/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.feature.detect.interest.EasyGeneralFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastQueue;

/**
 * Wrapper around {@link boofcv.alg.feature.detect.interest.GeneralFeatureDetector} to make it compatible with {@link InterestPointDetector}.
 *
 * @param <T> Input image type.
 * @param <D> Image derivative type.
 *
 * @author Peter Abeles
 */
public class GeneralToInterestPoint<T extends ImageGray, D extends ImageGray>
		extends EasyGeneralFeatureDetector<T,D>
		implements InterestPointDetector<T>
{

	double radius;

	// list of points it found
	protected FastQueue<Point2D_F64> foundPoints = new FastQueue<>(10, Point2D_F64.class, true);

	public GeneralToInterestPoint(GeneralFeatureDetector<T, D> detector,
								  double radius,
								  Class<T> imageType, Class<D> derivType) {
		super(detector,imageType,derivType);
		this.radius = radius;
	}

	public GeneralToInterestPoint(GeneralFeatureDetector<T, D> detector,
								  ImageGradient<T, D> gradient,
								  ImageHessian<D> hessian,
								  double radius,
								  Class<D> derivType) {
		super(detector, gradient, hessian, derivType);
		this.radius = radius;
	}

	@Override
	public void detect(T input) {
		super.detect(input,null);

		foundPoints.reset();
		if( getDetector().isDetectMaximums() ) {
			QueueCorner corners = detector.getMaximums();

			for (int i = 0; i < corners.size; i++) {
				Point2D_I16 p = corners.get(i);
				foundPoints.grow().set(p.x,p.y);
			}
		}

		if( getDetector().isDetectMinimums() ) {
			QueueCorner corners = detector.getMinimums();

			for (int i = 0; i < corners.size; i++) {
				Point2D_I16 p = corners.get(i);
				foundPoints.grow().set(p.x,p.y);
			}
		}

	}

	@Override
	public int getNumberOfFeatures() {
		return foundPoints.size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return foundPoints.get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return radius;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return 0;
	}

	@Override
	public boolean hasScale() {
		return false;
	}

	@Override
	public boolean hasOrientation() {
		return false;
	}
}
