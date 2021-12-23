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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;

/**
 * Except for very simple functions, computing the per pixel distortion is an expensive operation.
 * To overcome this problem the distortion is computed once and cached. Then when the image is distorted
 * again the save results are simply recalled and not computed again.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ImageDistortCache_SB<Input extends ImageGray<Input>, Output extends ImageGray<Output>>
		implements ImageDistort<Input, Output> {

	protected AssignPixelValue_SB<Output> assigner;

	// size of output image
	protected int width = -1, height = -1;
	protected Point2D_F32[] map;
	// sub pixel interpolation
	protected InterpolatePixelS<Input> interp;

	// transform
	protected PixelTransform<Point2D_F32> dstToSrc;

	// crop boundary
	protected int x0, y0, x1, y1;

	// should it render all pixels in the destination, even ones outside the input image
	protected boolean renderAll = true;
	protected Input srcImg;
	protected Output dstImg;

	protected boolean dirty;

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortCache_SB( AssignPixelValue_SB<Output> assigner,
								 InterpolatePixelS<Input> interp ) {
		this.assigner = assigner;
		this.interp = interp;
	}

	@Override
	public void setModel( PixelTransform<Point2D_F32> dstToSrc ) {
		this.dirty = true;
		this.dstToSrc = dstToSrc;
	}

	@Override
	public void apply( Input srcImg, Output dstImg ) {
		init(srcImg, dstImg);

		x0 = 0;
		y0 = 0;
		x1 = dstImg.width;
		y1 = dstImg.height;

		if (renderAll)
			renderAll();
		else
			applyOnlyInside();
	}

	@Override
	public void apply( Input srcImg, Output dstImg, GrayU8 mask ) {
		init(srcImg, dstImg);
		mask.reshape(dstImg);

		x0 = 0;
		y0 = 0;
		x1 = dstImg.width;
		y1 = dstImg.height;

		if (renderAll)
			renderAll(mask);
		else
			applyOnlyInside(mask);
	}

	@Override
	public void apply( Input srcImg, Output dstImg, int dstX0, int dstY0, int dstX1, int dstY1 ) {
		init(srcImg, dstImg);

		// Check that a valid region was specified. If not do nothing
		if (dstX1 <= dstX0 || dstY1 <= dstY0)
			return;

		x0 = dstX0;
		y0 = dstY0;
		x1 = dstX1;
		y1 = dstY1;

		if (renderAll)
			renderAll();
		else
			applyOnlyInside();
	}

	protected void init( Input srcImg, Output dstImg ) {
		if (dirty || width != dstImg.width || height != dstImg.height) {
			width = dstImg.width;
			height = dstImg.height;
			map = new Point2D_F32[width*height];
			for (int i = 0; i < map.length; i++) {
				map[i] = new Point2D_F32();
			}

			int index = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					dstToSrc.compute(x, y, map[index++]);
				}
			}
			dirty = false;
		} else if (dstImg.width != width || dstImg.height != height)
			throw new IllegalArgumentException("Unexpected dstImg dimension");

		this.srcImg = srcImg;
		this.dstImg = dstImg;
		interp.setImage(srcImg);
		assigner.setImage(dstImg);
	}

	protected void renderAll() {

		// todo TO make this faster first apply inside the region which can process the fast border
		// then do the slower border thingy
		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for (int x = x0; x < x1; x++, indexDst++) {
				Point2D_F32 s = map[indexDst];

				assigner.assign(indexDst, interp.get(s.x, s.y));
			}
		}
	}

	protected void renderAll( GrayU8 mask ) {
		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			int indexMsk = mask.startIndex + mask.stride*y + x0;

			for (int x = x0; x < x1; x++, indexDst++, indexMsk++) {
				Point2D_F32 s = map[indexDst];

				assigner.assign(indexDst, interp.get(s.x, s.y));
				if (s.x >= 0 && s.x <= maxWidth && s.y >= 0 && s.y <= maxHeight) {
					mask.data[indexMsk] = 1;
				} else {
					mask.data[indexMsk] = 0;
				}
			}
		}
	}

	protected void applyOnlyInside() {
		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for (int x = x0; x < x1; x++, indexDst++) {
				Point2D_F32 s = map[indexDst];

				if (s.x >= 0 && s.x <= maxWidth && s.y >= 0 && s.y <= maxHeight) {
					assigner.assign(indexDst, interp.get(s.x, s.y));
				}
			}
		}
	}

	protected void applyOnlyInside( GrayU8 mask ) {
		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			int indexMsk = mask.startIndex + mask.stride*y + x0;

			for (int x = x0; x < x1; x++, indexDst++, indexMsk++) {
				Point2D_F32 s = map[indexDst];

				if (s.x >= 0 && s.x <= maxWidth && s.y >= 0 && s.y <= maxHeight) {
					assigner.assign(indexDst, interp.get(s.x, s.y));
					mask.data[indexMsk] = 1;
				} else {
					mask.data[indexMsk] = 0;
				}
			}
		}
	}

	public Point2D_F32[] getMap() {
		return map;
	}

	public InterpolatePixelS<Input> getInterp() {
		return interp;
	}

	public PixelTransform<Point2D_F32> getDstToSrc() {
		return dstToSrc;
	}

	@Override
	public void setRenderAll( boolean renderAll ) {
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
