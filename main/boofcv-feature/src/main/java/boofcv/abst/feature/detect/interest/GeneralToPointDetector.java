/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

/**
 * Wrapper around {@link GeneralFeatureDetector} to make it compatible with {@link InterestPointDetector}.
 *
 * @param <T> Input image type.
 * @param <D> Image derivative type.
 *
 * @author Peter Abeles
 */
public class GeneralToPointDetector<T extends ImageGray<T>, D extends ImageGray<D>>
		extends EasyGeneralFeatureDetector<T,D>
		implements PointDetector<T>
{

	int totalSets;

	public GeneralToPointDetector(GeneralFeatureDetector<T, D> detector,
								  Class<T> imageType, Class<D> derivType) {
		super(detector,imageType,derivType);

		if( detector.isDetectMaximums())
			totalSets++;
		if( detector.isDetectMinimums())
			totalSets++;
	}

	public GeneralToPointDetector(GeneralFeatureDetector<T, D> detector,
								  ImageGradient<T, D> gradient,
								  ImageHessian<D> hessian,
								  Class<D> derivType) {
		super(detector, gradient, hessian, derivType);

		if( detector.isDetectMaximums())
			totalSets++;
		if( detector.isDetectMinimums())
			totalSets++;
	}

	@Override
	public void process(T input) {
		super.detect(input,null);
	}


	@Override
	public int totalSets() {
		return totalSets;
	}

	@Override
	public QueueCorner getPointSet(int which) {
		if( totalSets == 1) {
			if( detector.isDetectMinimums() )
				return detector.getMinimums();
			else
				return detector.getMaximums();
		} else if( which == 0 ) {
			return detector.getMinimums();
		} else if( which == 1 ) {
			return detector.getMaximums();
		} else {
			throw new IllegalArgumentException("Invalid set index");
		}
	}
}
