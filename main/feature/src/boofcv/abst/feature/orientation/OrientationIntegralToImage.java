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

import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;

/**
 * Converts an implementation of {@link OrientationIntegral} into {@link OrientationImage}.
 *
 * @author Peter Abeles
 */
public class OrientationIntegralToImage<T extends ImageGray, II extends ImageGray>
	implements OrientationImage<T>
{
	// algorithm which is being wrapped around
	OrientationIntegral<II> alg;

	// input image converted into an integral image
	II integralImage;

	// type of input image
	Class<T> inputType;

	public OrientationIntegralToImage(OrientationIntegral<II> alg,
									  Class<T> inputType ,
									  Class<II> integralType ) {
		this.alg = alg;
		this.inputType = inputType;
		integralImage = GeneralizedImageOps.createSingleBand(integralType, 1, 1);
	}

	@Override
	public void setImage(T image) {
		integralImage.reshape(image.width,image.height);
		GIntegralImageOps.transform(image, integralImage);
		alg.setImage(integralImage);
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
