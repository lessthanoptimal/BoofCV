/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageType;
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
public class GeneralToInterestPoint<T extends ImageGray<T>, D extends ImageGray<D>>
		extends EasyGeneralFeatureDetector<T,D>
		implements InterestPointDetector<T>
{
	int numSets;
	double radius;

	// Index at which it transitions from set 0 to set 1
	int indexOfSetSplit;

	// list of points it found
	protected FastQueue<Point2D_F64> foundPoints = new FastQueue<>(10, Point2D_F64::new);

	public GeneralToInterestPoint(GeneralFeatureDetector<T, D> detector,
								  double radius,
								  Class<T> imageType, Class<D> derivType) {
		super(detector,imageType,derivType);
		this.radius = radius;

		// Maximums and minimums are each considered a different set
		this.numSets = 0;
		if( detector.isDetectMinimums() )
			numSets++;
		if( detector.isDetectMinimums() )
			numSets++;
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

		if( numSets == 2 ) {
			// If there are two sets the maximums will be set 0 and minimums set 1
			indexOfSetSplit = detector.getMaximums().size;
		} else {
			indexOfSetSplit = Integer.MAX_VALUE;
		}
	}

	@Override
	public int getNumberOfSets() {
		return numSets;
	}

	@Override
	public int getSet(int index) {
		return index < indexOfSetSplit ? 0 : 1;
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

	@Override
	public ImageType<T> getInputType() {
		return ImageType.single(super.detector.getImageType());
	}
}
