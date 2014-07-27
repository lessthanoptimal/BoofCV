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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;

/**
 * Except for very simple functions, computing the per pixel distortion is an expensive operation.
 * To overcome this problem the distortion is computed once and cached.  Then when the image is distorted
 * again the save results are simply recalled and not computed again.
 *
 * @author Peter Abeles
 */
public abstract class ImageDistortCache<Input extends ImageSingleBand,Output extends ImageSingleBand>
		implements ImageDistort<Input,Output> {

	// size of output image
	private int width=-1,height=-1;
	private Point2D_F32 map[];
	// sub pixel interpolation
	private InterpolatePixelS<Input> interp;
	// handle the image border
	private ImageBorder<Input> border;

	// transform
	private PixelTransform_F32 dstToSrc;

	// crop boundary
	private int x0,y0,x1,y1;

	protected Input srcImg;
	protected Output dstImg;

	protected boolean dirty;

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 * @param border How borders are handled
	 */
	public ImageDistortCache(InterpolatePixelS<Input> interp,
							 ImageBorder<Input> border) {
		this.interp = interp;
		this.border = border;
	}

	@Override
	public void setModel(PixelTransform_F32 dstToSrc) {
		this.dirty = true;
		this.dstToSrc = dstToSrc;
	}

	@Override
	public void apply(Input srcImg, Output dstImg) {
		init(srcImg, dstImg);

		x0 = 0;y0 = 0;x1 = dstImg.width;y1 = dstImg.height;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	@Override
	public void apply(Input srcImg, Output dstImg, int dstX0, int dstY0, int dstX1, int dstY1) {
		init(srcImg, dstImg);

		x0 = dstX0;y0 = dstY0;x1 = dstX1;y1 = dstY1;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	private void init(Input srcImg, Output dstImg) {
		if( dirty || width != dstImg.width || height != dstImg.height) {
			width = dstImg.width;
			height = dstImg.height;
			map = new Point2D_F32[width*height];
			for( int i = 0; i < map.length; i++ ) {
				map[i] = new Point2D_F32();
			}

			int index = 0;
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					dstToSrc.compute(x,y);
					map[index++].set(dstToSrc.distX,dstToSrc.distY);
				}
			}
		} else if( dstImg.width != width || dstImg.height != height )
			throw new IllegalArgumentException("Unexpected dstImg dimension");

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
				Point2D_F32 s = map[indexDst];

				if( s.x < minInterpX || s.x > maxInterpX || s.y < minInterpY || s.y > maxInterpY ) {
					if( s.x < 0f || s.x > widthF || s.y < 0f || s.y > heightF )
						assign(indexDst,(float)border.getGeneral((int)s.x,(int)s.y));
					else
						assign(indexDst,interp.get(s.x, s.y));
				} else {
					assign(indexDst,interp.get_fast(s.x, s.y));
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
				Point2D_F32 s = map[indexDst];

				if( s.x < minInterpX || s.x > maxInterpX || s.y < minInterpY || s.y > maxInterpY ) {
					if( s.x >= 0f && s.x <= widthF && s.y >= 0f && s.y <= heightF )
						assign(indexDst,interp.get(s.x, s.y));
				} else {
					assign(indexDst,interp.get_fast(s.x, s.y));
				}
			}
		}
	}

	protected abstract void assign( int indexDst , float value );
}
