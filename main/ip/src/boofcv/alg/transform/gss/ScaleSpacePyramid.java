/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.gss;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.transform.pyramid.PyramidUpdateGaussianScale;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidFloat;


/**
 * <p>
 * A pyramidal representation of {@link boofcv.struct.gss.GaussianScaleSpace scale-space}.  Each
 * layer in the pyramid is created by applying a Gaussian blur to the previous layer and then sub-sampling.
 * The amount of blur applied to each level is selected such that it has the same effective amount of
 * blurring as {@link boofcv.struct.gss.GaussianScaleSpace} at the same level.
 * </p>
 *
 * <p>
 * NOTES:<br>
 * <ul>
 * <li>Interpolation by default is bilinear, which can be overridden.</li>
 * <li>By visual inspection it appears to add in a bit of extra blur at higher layers.</li> 
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class ScaleSpacePyramid<T extends ImageSingleBand> extends PyramidFloat<T> {

	// updates each layer in the pyramid by applying Gaussian blur and then sub-sampling it.
	protected PyramidUpdateGaussianScale<T> updater;

	/**
	 * Creates a scale-space pyramid where each layer in the pyramid is designed to produce a similar
	 * response as a layer in a {@link boofcv.struct.gss.GaussianScaleSpace}.  Each layer's scale is set to the same
	 * value as the scaleSpace, so the resolution is constantly decreasing.  The blur applied to each
	 * layer is computed to apply the same amount of blur as would be present in the desired scale space.
	 *
	 * @param scaleSpace Effective total blur/scale space of each layer.
	 */
	public ScaleSpacePyramid(Class<T> imageType, double ...scaleSpace) {
		super(imageType);

		if( scaleSpace.length <= 0 )
			throw new IllegalArgumentException("Input arrays must be >= 1");

		setScaleFactors(scaleSpace);
		double[] sigmas = new double[ scaleSpace.length ];

		sigmas[0] = scaleSpace[0];
		for( int i = 1; i < scaleSpace.length; i++ ) {
			// the desired amount of blur
			double c = scale[i];
			// the effective amount of blur applied to the last level
			double b = scale[i-1];
			// the amount of additional blur which is needed
			sigmas[i] = Math.sqrt(c*c-b*b);
			// take in account the previous image being rescaled
			sigmas[i] /= scale[i-1];
		}

		InterpolatePixel<T> interpolate = FactoryInterpolation.bilinearPixel(imageType);
		updater = new PyramidUpdateGaussianScale(interpolate,sigmas);
	}

	public void setInterpolation( InterpolatePixel<T> interpolate ) {
		updater.setInterpolate(interpolate);
	}

	public void setImage( T image ) {
		updater.update(image,this);
	}

	/**
	 * Relative scale of the image at this layer.
	 *
	 * @param layer
	 * @return
	 */
	@Override
	public double getScale(int layer) {
		return scale[layer];
	}

	public PyramidUpdateGaussianScale<T> getUpdater() {
		return updater;
	}
}
