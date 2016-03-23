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

package boofcv.abst.feature.orientation;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;

/**
 * Converts an implementation of {@link OrientationGradient} into {@link OrientationImage}.
 *
 * @author Peter Abeles
 */
public class OrientationGradientToImage<T extends ImageGray, D extends ImageGray>
	implements OrientationImage<T>
{
	ImageGradient<T,D> gradient;
	OrientationGradient<D> alg;

	// storage for image gradient
	D derivX;
	D derivY;

	// Input image type
	Class<T> inputType;

	public OrientationGradientToImage(OrientationGradient<D> alg,
									  ImageGradient<T, D> gradient,
									  Class<T> inputType ,
									  Class<D> gradientType ) {
		this.alg = alg;
		this.gradient = gradient;
		this.inputType = inputType;

		derivX = GeneralizedImageOps.createSingleBand(gradientType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(gradientType,1,1);
	}

	@Override
	public void setImage(T image) {
		derivX.reshape(image.width,image.height);
		derivY.reshape(image.width,image.height);

		gradient.process(image,derivX,derivY);
		alg.setImage(derivX,derivY);
	}

	@Override
	public Class<T> getImageType() {
		return inputType;
	}

	@Override
	public void setObjectRadius(double radius) {
		alg.setObjectRadius(radius);
	}

	@Override
	public double compute(double c_x, double c_y) {
		return alg.compute(c_x,c_y);
	}
}
