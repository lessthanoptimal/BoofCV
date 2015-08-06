/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.image.ImageInterleaved;

/**
 * Most basic implementation of {@link ImageDistort} for {@link ImageInterleaved}. Computes the distortion from the
 * dst to src image for each pixel.  The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
public abstract class ImageDistortBasic_IL<Input extends ImageInterleaved,Output extends ImageInterleaved>
		extends ImageDistortBasic<Input,Output,InterpolatePixelMB<Input>> {

	// storage for interpolated pixel values
	float values[] = new float[0];

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortBasic_IL(InterpolatePixelMB<Input> interp) {
		super(interp);
	}

	@Override
	protected void init(Input srcImg, Output dstImg) {
		super.init(srcImg,dstImg);
		if( values.length != srcImg.getNumBands() ) {
			values = new float[ srcImg.getNumBands() ];
		}
	}

	@Override
	public void applyAll() {

		// todo TO make this faster first apply inside the region which can process the fast border
		// then do the slower border thingy
		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			for( int x = x0; x < x1; x++ , indexDst += dstImg.numBands ) {
				dstToSrc.compute(x,y);

				interp.get(dstToSrc.distX, dstToSrc.distY, values);
				assign(indexDst,values);
			}
		}
	}

	@Override
	public void applyOnlyInside() {

		float maxWidth = srcImg.getWidth()-1;
		float maxHeight = srcImg.getHeight()-1;

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			for( int x = x0; x < x1; x++ , indexDst += dstImg.numBands ) {
				dstToSrc.compute(x,y);

				if( dstToSrc.distX >= 0 && dstToSrc.distX <= maxWidth &&
						dstToSrc.distY >= 0 && dstToSrc.distY <= maxHeight ) {
					interp.get(dstToSrc.distX, dstToSrc.distY, values);
					assign(indexDst,values);
				}
			}
		}
	}

	protected abstract void assign( int indexDst , float[] value );
}
