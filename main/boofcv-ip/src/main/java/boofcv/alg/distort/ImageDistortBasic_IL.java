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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageInterleaved;
import georegression.struct.point.Point2D_F32;

/**
 * Most basic implementation of {@link ImageDistort} for {@link ImageInterleaved}. Computes the distortion from the
 * dst to src image for each pixel. The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
public class ImageDistortBasic_IL
		<Input extends ImageInterleaved<Input>, Output extends ImageInterleaved<Output>>
		extends ImageDistortBasic<Input, Output, InterpolatePixelMB<Input>> {

	AssignPixelValue_MB<Output> assigner;

	// storage for interpolated pixel values
	float[] values = new float[0];

	Point2D_F32 distorted = new Point2D_F32();

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortBasic_IL( AssignPixelValue_MB<Output> assigner,
								 InterpolatePixelMB<Input> interp ) {
		super(interp);
		this.assigner = assigner;
	}

	@Override
	protected void init( Input srcImg, Output dstImg ) {
		super.init(srcImg, dstImg);
		if (values.length != srcImg.getNumBands()) {
			values = new float[srcImg.getNumBands()];
		}
		assigner.setImage(dstImg);
	}

	@Override
	protected void applyAll() {

		// todo TO make this faster first apply inside the region which can process the fast border
		// then do the slower border thingy
		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			for (int x = x0; x < x1; x++, indexDst += dstImg.numBands) {
				dstToSrc.compute(x, y, distorted);
				interp.get(distorted.x, distorted.y, values);
				assigner.assign(indexDst, values);
			}
		}
	}

	@Override
	protected void applyAll( GrayU8 mask ) {

		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			int indexMsk = mask.startIndex + mask.stride*y + x0;

			for (int x = x0; x < x1; x++, indexDst += dstImg.numBands, indexMsk++) {
				dstToSrc.compute(x, y, distorted);
				interp.get(distorted.x, distorted.y, values);

				assigner.assign(indexDst, values);

				if (distorted.x >= 0 && distorted.x <= maxWidth &&
						distorted.y >= 0 && distorted.y <= maxHeight) {
					mask.data[indexMsk] = 1;
				} else {
					mask.data[indexMsk] = 0;
				}
			}
		}
	}

	@Override
	protected void applyOnlyInside() {

		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			for (int x = x0; x < x1; x++, indexDst += dstImg.numBands) {
				dstToSrc.compute(x, y, distorted);

				if (distorted.x >= 0 && distorted.x <= maxWidth &&
						distorted.y >= 0 && distorted.y <= maxHeight) {
					interp.get(distorted.x, distorted.y, values);
					assigner.assign(indexDst, values);
				}
			}
		}
	}

	@Override
	protected void applyOnlyInside( GrayU8 mask ) {

		float maxWidth = srcImg.getWidth() - 1;
		float maxHeight = srcImg.getHeight() - 1;

		for (int y = y0; y < y1; y++) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0*dstImg.numBands;
			int indexMsk = mask.startIndex + mask.stride*y + x0;

			for (int x = x0; x < x1; x++, indexDst += dstImg.numBands, indexMsk++) {
				dstToSrc.compute(x, y, distorted);

				if (distorted.x >= 0 && distorted.x <= maxWidth &&
						distorted.y >= 0 && distorted.y <= maxHeight) {
					interp.get(distorted.x, distorted.y, values);
					assigner.assign(indexDst, values);
					mask.data[indexMsk] = 1;
				} else {
					mask.data[indexMsk] = 0;
				}
			}
		}
	}
}
