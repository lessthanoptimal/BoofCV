/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.misc.impl;

import gecv.alg.InputSanityCheck;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.misc.ImageDistort;
import gecv.struct.distort.PixelDistort;
import gecv.struct.image.ImageFloat32;


/**
 * <p>Implementation of {@link gecv.alg.misc.ImageDistort}.</p>
 *
 * <p>
 * DO NOT MODIFY: Generated by {@link gecv.alg.misc.impl.GeneratorImageDistort}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class ImageDistort_F32 implements ImageDistort<ImageFloat32> {

	// transform from dst to src image
	private PixelDistort dstToSrc;
	// sub pixel interpolation
	private InterpolatePixel<ImageFloat32> interp;

	public ImageDistort_F32(PixelDistort dstToSrc, InterpolatePixel<ImageFloat32> interp) {
		this.dstToSrc = dstToSrc;
		this.interp = interp;
	}

	@Override
	public void apply( ImageFloat32 srcImg , ImageFloat32 dstImg ) {
		InputSanityCheck.checkSameShape(srcImg,dstImg);

		interp.setImage(srcImg);

		final int width = srcImg.getWidth();
		final int height = srcImg.getHeight();

		final float widthF = width;
		final float heightF = height;

		for( int y = 0; y < height; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y;
			for( int x = 0; x < width; x++ , indexDst++ ) {
				dstToSrc.distort(x,y);

				final float sx = dstToSrc.distX;
				final float sy = dstToSrc.distY;

				if( sx < 0f || sx >= widthF || sy < 0f || sy >= heightF ) {
					continue;
				}

				dstImg.data[indexDst] = interp.get_unsafe(sx,sy);
			}
		}
	}

	@Override
	public void apply( ImageFloat32 srcImg , ImageFloat32 dstImg , Number value ) {
		InputSanityCheck.checkSameShape(srcImg,dstImg);

		interp.setImage(srcImg);

		float valueF = value.floatValue();

		final int width = srcImg.getWidth();
		final int height = srcImg.getHeight();

		final float widthF = width;
		final float heightF = height;

		for( int y = 0; y < height; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y;
			for( int x = 0; x < width; x++ , indexDst++ ) {
				dstToSrc.distort(x,y);

				final float sx = dstToSrc.distX;
				final float sy = dstToSrc.distY;

				if( sx < 0f || sx >= widthF || sy < 0f || sy >= heightF ) {
					dstImg.data[indexDst] = valueF;
					continue;
				}

				dstImg.data[indexDst] = interp.get_unsafe(sx,sy);
			}
		}
	}

}
