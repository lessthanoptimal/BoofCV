/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;

/**
 * Most basic implementation of {@link ImageDistort}. Computes the distortion from the dst to src image
 * for each pixel. The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ImageDistortBasic
		<Input extends ImageBase<Input>,Output extends ImageBase<Output>,Interpolate extends InterpolatePixel<Input>>
		implements ImageDistort<Input,Output> {

	// distortion model from the dst to src image
	protected PixelTransform<Point2D_F32> dstToSrc;
	// sub pixel interpolation
	protected Interpolate interp;

	// crop boundary
	protected int x0,y0,x1,y1;

	// should it render all pixels in the destination, even ones outside the input image
	protected boolean renderAll = true;
	protected Input srcImg;
	protected Output dstImg;

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	protected ImageDistortBasic( Interpolate interp ) {
		this.interp = interp;
	}

	@Override
	public void setModel(PixelTransform<Point2D_F32> dstToSrc) {
		this.dstToSrc = dstToSrc;
	}

	@Override
	public void apply(Input srcImg, Output dstImg) {
		init(srcImg, dstImg);

		x0 = 0;y0 = 0;x1 = dstImg.width;y1 = dstImg.height;

		if(renderAll)
			applyAll();
		else
			applyOnlyInside();
	}

	@Override
	public void apply(Input srcImg, Output dstImg, GrayU8 mask ) {
		init(srcImg, dstImg);
		mask.reshape(dstImg);

		x0 = 0;y0 = 0;x1 = dstImg.width;y1 = dstImg.height;

		if(renderAll)
			applyAll(mask);
		else
			applyOnlyInside(mask);
	}

	@Override
	public void apply(Input srcImg, Output dstImg, int dstX0, int dstY0, int dstX1, int dstY1) {
		init(srcImg, dstImg);

		// Check that a valid region was specified. If not do nothing
		if( dstX1 <= dstX0 || dstY1 <= dstY0 )
			return;

		x0 = dstX0;y0 = dstY0;x1 = dstX1;y1 = dstY1;

		if(renderAll)
			applyAll();
		else
			applyOnlyInside();
	}

	protected void init(Input srcImg, Output dstImg) {
		this.srcImg = srcImg;
		this.dstImg = dstImg;
		interp.setImage(srcImg);
	}

	protected abstract void applyAll();

	protected abstract void applyAll(GrayU8 mask);

	protected abstract void applyOnlyInside();

	protected abstract void applyOnlyInside( GrayU8 mask );

	@Override
	public void setRenderAll(boolean renderAll) {
		this.renderAll = renderAll;
	}

	@Override
	public boolean getRenderAll() {
		return renderAll;
	}

	@Override
	public PixelTransform<Point2D_F32> getModel() {
		return dstToSrc;
	}
}
