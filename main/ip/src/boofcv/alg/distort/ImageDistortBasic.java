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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;

/**
 * Most basic implementation of {@link ImageDistort}. Computes the distortion from the dst to src image
 * for each pixel.  The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
public abstract class ImageDistortBasic<T extends ImageSingleBand> implements ImageDistort<T> {

	// distortion model from the dst to src image
	protected PixelTransform_F32 dstToSrc;
	// sub pixel interpolation
	protected InterpolatePixelS<T> interp;
	// handle the image border
	protected ImageBorder<T> border;

	// crop boundary
	protected int x0,y0,x1,y1;

	protected T srcImg;
	protected T dstImg;

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 * @param border How borders are handled
	 */
	public ImageDistortBasic(InterpolatePixelS<T> interp,
							 ImageBorder<T> border) {
		this.interp = interp;
		this.border = border;
	}

	@Override
	public void setModel(PixelTransform_F32 dstToSrc) {
		this.dstToSrc = dstToSrc;
	}

	@Override
	public void apply(T srcImg, T dstImg) {
		init(srcImg, dstImg);

		x0 = 0;y0 = 0;x1 = dstImg.width;y1 = dstImg.height;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	@Override
	public void apply(T srcImg, T dstImg, int dstX0, int dstY0, int dstX1, int dstY1) {
		init(srcImg, dstImg);

		x0 = dstX0;y0 = dstY0;x1 = dstX1;y1 = dstY1;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	private void init(T srcImg, T dstImg) {
		this.srcImg = srcImg;
		this.dstImg = dstImg;
		interp.setImage(srcImg);
	}

	public void applyBorder() {

		border.setImage(srcImg);

		final float minInterpX = interp.getFastBorderX();
		final float minInterpY = interp.getFastBorderY();
		final float maxInterpX = srcImg.getWidth()-interp.getFastBorderX()-1;
		final float maxInterpY = srcImg.getHeight()-interp.getFastBorderY()-1;

		final float widthF = srcImg.getWidth()-1;
		final float heightF = srcImg.getHeight()-1;

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				dstToSrc.compute(x,y);

				if( dstToSrc.distX < minInterpX || dstToSrc.distX > maxInterpX ||
						dstToSrc.distY < minInterpY || dstToSrc.distY > maxInterpY ) {
					if( dstToSrc.distX < 0f || dstToSrc.distX > widthF || dstToSrc.distY < 0f || dstToSrc.distY > heightF )
						assign(indexDst,(float)border.getGeneral((int)dstToSrc.distX,(int)dstToSrc.distY));
					else
						assign(indexDst,interp.get(dstToSrc.distX, dstToSrc.distY));
				} else {
					assign(indexDst,interp.get_fast(dstToSrc.distX, dstToSrc.distY));
				}
			}
		}
	}

	public void applyNoBorder() {
		final float minInterpX = interp.getFastBorderX();
		final float minInterpY = interp.getFastBorderY();
		final float maxInterpX = srcImg.getWidth()-interp.getFastBorderX()-1;
		final float maxInterpY = srcImg.getHeight()-interp.getFastBorderY()-1;

		final float widthF = srcImg.getWidth()-1;
		final float heightF = srcImg.getHeight()-1;

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				dstToSrc.compute(x,y);

				if( dstToSrc.distX < minInterpX || dstToSrc.distX > maxInterpX ||
						dstToSrc.distY < minInterpY || dstToSrc.distY > maxInterpY ) {
					if( dstToSrc.distX >= 0f && dstToSrc.distX <= widthF && dstToSrc.distY >= 0f && dstToSrc.distY <= heightF )
						assign(indexDst,interp.get(dstToSrc.distX, dstToSrc.distY));
				} else {
					assign(indexDst,interp.get_fast(dstToSrc.distX, dstToSrc.distY));
				}
			}
		}
	}

	protected abstract void assign( int indexDst , float value );
}
