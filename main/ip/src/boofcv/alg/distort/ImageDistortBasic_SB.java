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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.ImageGray;

/**
 * Most basic implementation of {@link ImageDistort} for {@link ImageGray}. Computes the distortion from the
 * dst to src image for each pixel.  The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
public abstract class ImageDistortBasic_SB<Input extends ImageGray,Output extends ImageGray>
		extends ImageDistortBasic<Input,Output,InterpolatePixelS<Input>> {

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortBasic_SB(InterpolatePixelS<Input> interp) {
		super(interp);
	}

	@Override
	public void applyAll() {

		// todo TO make this faster first apply inside the region which can process the fast border
		// then do the slower border thingy
		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				dstToSrc.compute(x,y);

				assign(indexDst,interp.get(dstToSrc.distX, dstToSrc.distY));
			}
		}
	}

	@Override
	public void applyOnlyInside() {

		float maxWidth = srcImg.getWidth()-1;
		float maxHeight = srcImg.getHeight()-1;

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				dstToSrc.compute(x,y);

				if( dstToSrc.distX >= 0 && dstToSrc.distX <= maxWidth &&
						dstToSrc.distY >= 0 && dstToSrc.distY <= maxHeight ) {
					assign(indexDst,interp.get(dstToSrc.distX, dstToSrc.distY));
				}
			}
		}
	}

	protected abstract void assign( int indexDst , float value );
}
