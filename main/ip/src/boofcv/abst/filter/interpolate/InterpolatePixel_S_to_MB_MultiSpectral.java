/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.interpolate;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * Wrapper around {@link InterpolatePixelS} which allows it to interpolate {@link MultiSpectral} inside
 * a {@link InterpolatePixelMB}.  Performs the same calculations multiple times compared to a custom solution.
 *
 * @author Peter Abeles
 */
public class InterpolatePixel_S_to_MB_MultiSpectral<T extends ImageSingleBand>
		implements InterpolatePixelMB<MultiSpectral<T>>
{
	InterpolatePixelS<T> alg;

	MultiSpectral<T> image;

	public InterpolatePixel_S_to_MB_MultiSpectral(InterpolatePixelS<T> alg) {
		this.alg = alg;
	}

	@Override
	public void get(float x, float y, float[] values) {
		final int N = image.getNumBands();
		for( int i = 0; i < N; i++ ) {
			alg.setImage(image.getBand(i));
			values[i] = alg.get(x,y);
		}
	}

	@Override
	public void get_fast(float x, float y, float[] values) {
		final int N = image.getNumBands();
		for( int i = 0; i < N; i++ ) {
			alg.setImage(image.getBand(i));
			values[i] = alg.get_fast(x,y);
		}
	}

	@Override
	public void setImage(MultiSpectral<T> image) {
		this.image = image;
		// set it to use the first band by default so that other functions can work
		alg.setImage( image.getBand(0));
	}

	@Override
	public MultiSpectral<T> getImage() {
		return image;
	}

	@Override
	public boolean isInFastBounds(float x, float y) {
		return alg.isInFastBounds(x,y);
	}

	@Override
	public int getFastBorderX() {
		return alg.getFastBorderX();
	}

	@Override
	public int getFastBorderY() {
		return alg.getFastBorderY();
	}

	@Override
	public ImageType<MultiSpectral<T>> getImageType() {
		throw new RuntimeException("Image type isn't determined until it processes an image");
	}
}
