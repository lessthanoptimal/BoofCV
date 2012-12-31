/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidFloat;
import boofcv.struct.pyramid.PyramidUpdater;
import boofcv.struct.pyramid.PyramidUpdaterFloat;


/**
 * <p>
 * {@link PyramidUpdater> for {@link ImagePyramid}s where each layer is first blurred using
 * a Gaussian filter and then downsampled using interpolation.  The scaling factor between
 * each level are floating point number.  Unlike {@link boofcv.alg.transform.pyramid.PyramidUpdateIntegerDown}
 * the scale factors can be arbitrary and are not limited to certain integer values.  The specified
 * sigmas are the sigmas which are applied to each layer.
 * </p>
 *
 * <p>
 *
 * </p>
 *
 * <p>
 * NOTE: This can be considered the theoretically correct way to construct an image pyramid
 * with no sacrifices to improve speed.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidUpdateGaussianScale< T extends ImageSingleBand> implements PyramidUpdaterFloat<T> {

	// interpolation algorithm
	protected InterpolatePixel<T> interpolate;

	// used to store the blurred image
	protected T tempImage;

	// how much each layer is blurred before sub-sampling
	protected float[] sigmas;

	/**
	 * Creates the updater.
	 *
	 * @param interpolate Interpolation function used to sub-sample.
	 * @param sigmas Amount of blur applied to each layer in the pyramid.
	 */
	public PyramidUpdateGaussianScale(InterpolatePixel<T> interpolate, double ...sigmas ) {
		this.interpolate = interpolate;
		this.sigmas = new float[ sigmas.length ];
		for( int i = 0; i < sigmas.length; i++ )
			this.sigmas[i] = (float)sigmas[i];
	}

	@Override
	public void update(T input, PyramidFloat<T> pyramid) {
		if( !pyramid.isInitialized() ||
				pyramid.getInputWidth() != input.width ||
				pyramid.getInputHeight() != input.height )
			pyramid.initialize(input.width,input.height);

		if( pyramid.isSaveOriginalReference() )
			throw new IllegalArgumentException("The original reference cannot be saved");

		if( tempImage == null ) {
			tempImage = (T)input._createNew(input.width,input.height);
		}

		for( int i = 0; i < pyramid.scale.length; i++ ) {
			T prev = i == 0 ? input : pyramid.getLayer(i-1);
			T layer = pyramid.getLayer(i);


			BlurStorageFilter<T> blur = (BlurStorageFilter<T>) FactoryBlurFilter.gaussian(layer.getClass(),sigmas[i],-1);

			tempImage.reshape(prev.width,prev.height);
			blur.process(prev,tempImage);

			PixelTransformAffine_F32 model = DistortSupport.transformScale(layer,tempImage);
			DistortImageOps.distortSingle(tempImage,layer,model,null,interpolate);
		}
	}

	public InterpolatePixel<T> getInterpolate() {
		return interpolate;
	}

	public void setInterpolate(InterpolatePixel<T> interpolate) {
		this.interpolate = interpolate;
	}
}
