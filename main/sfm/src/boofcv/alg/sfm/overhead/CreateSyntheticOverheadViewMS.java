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

package boofcv.alg.sfm.overhead;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F32;

/**
 * Implementation of {@link CreateSyntheticOverheadView} for {@link MultiSpectral}.
 *
 * @author Peter Abeles
 */
public class CreateSyntheticOverheadViewMS<T extends ImageSingleBand>
		extends CreateSyntheticOverheadView<MultiSpectral<T>>
{
	// computes interpolated pixel value.
	// have one for each band so that you don't need to constantly change the image it's set to
	private InterpolatePixelS<T> interp[];

	// local variables
	private GImageSingleBand output[];

	/**
	 * Constructor which allows the interpolator for each band to be specified
	 *
	 * @param interp Interpolator for each band
	 */
	public CreateSyntheticOverheadViewMS(InterpolatePixelS<T> interp[]) {
		this.interp = interp;
		output = new GImageSingleBand[interp.length];
	}

	/**
	 * Constructor which allows the type of interpolation to be specified.
	 *
	 * @param type Type of interpolation used
	 * @param numBands Number of bands in the image.
	 * @param imageType Image of each band
	 */
	public CreateSyntheticOverheadViewMS( TypeInterpolate type , int numBands , Class<T> imageType ) {
		this.interp = new InterpolatePixelS[numBands];
		for( int i = 0; i < numBands; i++ ) {
			interp[i] = FactoryInterpolation.createPixelS(0, 255, type, imageType);
		}
		output = new GImageSingleBand[interp.length];
	}

	/**
	 * Computes overhead view of input image.  All pixels in input image are assumed to be on the ground plane.
	 *
	 * @param input (Input) Camera image.
	 * @param output (Output) Image containing overhead view.
	 */
	public void process(MultiSpectral<T> input, MultiSpectral<T> output) {

		int N = input.getNumBands();
		for( int i = 0; i < N; i++ ) {
			this.output[i] = FactoryGImageSingleBand.wrap(output.getBand(i),this.output[i]);
			interp[i].setImage(input.getBand(i));
		}

		int indexMap = 0;
		for( int i = 0; i < output.height; i++ ) {
			int indexOut = output.startIndex + i*output.stride;
			for( int j = 0; j < output.width; j++ , indexOut++,indexMap++ ) {
				Point2D_F32 p = mapPixels[indexMap];
				if( p != null ) {
					for( int k = 0; k < N; k++ ) {
						this.output[k].set(indexOut, interp[k].get( p.x, p.y));
					}
				}
			}
		}
	}
}
