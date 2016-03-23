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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Several corner detector algorithms work by computing a symmetric matrix whose elements are composed of the convolution
 * of the image's gradient squared.  This is done for X*X, X*Y, and X*X.  Once the matrix has been constructed
 * it is used to estimate how corner like the pixel under consideration is.  This class provides a generalized
 * interface for performing these calculations in an optimized manor.
 * </p>
 *
 * <p>
 * NOTE: Image borders are not processed.  The zeros in the image border need to be taken in account when
 * extract features using algorithms such as non-max suppression.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImplSsdCornerBase<D extends ImageGray, D2 extends ImageGray>
		implements GradientCornerIntensity<D>
{
	// input image gradient
	protected D derivX;
	protected D derivY;

	// radius of detected features
	protected int radius;

	// temporary storage for intensity derivatives summations
	protected D2 horizXX;
	protected D2 horizXY;
	protected D2 horizYY;

	// used to keep track of where it is in the image
	protected int x, y;

	public ImplSsdCornerBase( int windowRadius , Class<D2> secondDerivType ) {
		this.radius = windowRadius;

		horizXX = GeneralizedImageOps.createSingleBand(secondDerivType,1,1);
		horizXY = GeneralizedImageOps.createSingleBand(secondDerivType,1,1);
		horizYY = GeneralizedImageOps.createSingleBand(secondDerivType,1,1);
	}

	public void setImageShape( int imageWidth, int imageHeight ) {
		horizXX.reshape(imageWidth,imageHeight);
		horizYY.reshape(imageWidth,imageHeight);
		horizXY.reshape(imageWidth,imageHeight);
	}

	@Override
	public int getRadius() {
		return radius;
	}

	/**
	 * Computes the pixel's corner intensity.
	 * @return corner intensity.
	 */
	protected abstract float computeIntensity();

	@Override
	public int getIgnoreBorder() {
		return radius;
	}

	@Override
	public void process(D derivX, D derivY, GrayF32 intensity ) {
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		setImageShape(derivX.getWidth(),derivX.getHeight());
		this.derivX = derivX;
		this.derivY = derivY;

		// there is no intensity computed along the border. Make sure it's always zero
		// In the future it might be better to fill it with meaningful data, even if it's
		// from a partial region
		ImageMiscOps.fillBorder(intensity,0,radius);
		horizontal();
		vertical(intensity);
	}

	protected abstract void horizontal();

	protected abstract void vertical(GrayF32 intensity);

}
