/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofDefaults;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.interest.UnrollSiftScaleSpaceGradient;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around {@link OrientationHistogramSift} for {@link OrientationImage}. Selects
 * the best solution from the multiple solutions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class OrientationSiftToImage<T extends ImageGray<T>>
		implements OrientationImage<T> {
	UnrollSiftScaleSpaceGradient gradient = new UnrollSiftScaleSpaceGradient();
	OrientationHistogramSift<GrayF32> alg;
	UnrollSiftScaleSpaceGradient.ImageScale image;
	double sigma = 1.0/BoofDefaults.SIFT_SCALE_TO_RADIUS;

	Class<T> imageType;
	GrayF32 imageFloat = new GrayF32(1, 1);
	SiftScaleSpace ss;

	public OrientationSiftToImage( OrientationHistogramSift<GrayF32> alg,
								   SiftScaleSpace ss, Class<T> imageType ) {
		this.alg = alg;
		this.ss = ss;
		this.imageType = imageType;

		gradient.initialize(ss);
	}

	@Override
	public void setImage( T image ) {
		GrayF32 input;
		if (image instanceof GrayF32) {
			input = (GrayF32)image;
		} else {
			imageFloat.reshape(image.width, image.height);
			GConvertImage.convert(image, imageFloat);
			input = imageFloat;
		}

		ss.process(input);
		gradient.process(ss);
		setObjectRadius(sigma*BoofDefaults.SIFT_SCALE_TO_RADIUS);
	}

	@Override
	public Class<T> getImageType() {
		return imageType;
	}

	@Override
	public void setObjectRadius( double radius ) {
		sigma = radius/BoofDefaults.SIFT_SCALE_TO_RADIUS;
		this.image = gradient.lookup(sigma);
	}

	@Override
	public double compute( double c_x, double c_y ) {
		alg.setImageGradient(image.derivX, image.derivY);

		double imageToInput = image.imageToInput;
		alg.process(c_x/imageToInput, c_y/imageToInput, sigma/imageToInput);

		return alg.getPeakOrientation();
	}

	@Override
	public RegionOrientation copy() {
		throw new RuntimeException("IMplement");
	}
}
