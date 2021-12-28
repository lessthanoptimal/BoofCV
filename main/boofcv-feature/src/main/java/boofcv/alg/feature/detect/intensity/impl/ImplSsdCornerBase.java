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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Several corner detector algorithms work by computing a symmetric matrix whose elements are composed of the convolution
 * of the image's gradient squared. This is done for X*X, X*Y, and X*X. Once the matrix has been constructed
 * it is used to estimate how corner like the pixel under consideration is. This class provides a generalized
 * interface for performing these calculations in an optimized manor.
 * </p>
 *
 * <p>
 * NOTE: Image borders are not processed. The zeros in the image border need to be taken in account when
 * extract features using algorithms such as non-max suppression.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ImplSsdCornerBase<D extends ImageGray<D>, D2 extends ImageGray<D2>>
		implements GradientCornerIntensity<D> {
	// input image gradient
	protected D derivX;
	protected D derivY;

	// radius of detected features
	protected int radius;

	// temporary storage for intensity derivatives summations
	protected D2 horizXX;
	protected D2 horizXY;
	protected D2 horizYY;

	Class<D> derivType;

	protected ImplSsdCornerBase( int windowRadius, Class<D> derivType, Class<D2> secondDerivType ) {
		this.radius = windowRadius;
		this.derivType = derivType;

		horizXX = GeneralizedImageOps.createSingleBand(secondDerivType, 1, 1);
		horizXY = GeneralizedImageOps.createSingleBand(secondDerivType, 1, 1);
		horizYY = GeneralizedImageOps.createSingleBand(secondDerivType, 1, 1);
	}

	protected void setImageShape( int imageWidth, int imageHeight ) {
		horizXX.reshape(imageWidth, imageHeight);
		horizYY.reshape(imageWidth, imageHeight);
		horizXY.reshape(imageWidth, imageHeight);
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public int getIgnoreBorder() {
		return radius;
	}

	@Override
	public Class<D> getInputType() {
		return derivType;
	}

	public interface CornerIntensity_S32 {
		/**
		 * Computes the pixel's corner intensity.
		 *
		 * @return corner intensity.
		 */
		float compute( int totalXX, int totalXY, int totalYY );
	}

	public interface CornerIntensity_F32 {
		/**
		 * Computes the pixel's corner intensity.
		 *
		 * @return corner intensity.
		 */
		float compute( float totalXX, float totalXY, float totalYY );
	}
}
