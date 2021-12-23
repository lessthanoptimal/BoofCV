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
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageInterleaved;
import georegression.struct.point.Point2D_F32;

import java.util.ArrayDeque;

/**
 * Most basic implementation of {@link ImageDistort} for {@link ImageInterleaved}. Computes the distortion from the
 * dst to src image for each pixel. The dst pixel value is then set to the interpolated value of the src image.
 *
 * @author Peter Abeles
 */
public class ImageDistortBasic_IL_MT
		<Input extends ImageInterleaved<Input>, Output extends ImageInterleaved<Output>>
		extends ImageDistortBasic<Input, Output, InterpolatePixelMB<Input>> {

	private AssignPixelValue_MB<Output> assigner;

	private final ArrayDeque<BlockDistort> queue = new ArrayDeque<>();

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortBasic_IL_MT( AssignPixelValue_MB<Output> assigner,
									InterpolatePixelMB<Input> interp ) {
		super(interp);
		this.assigner = assigner;
	}

	private BlockDistort pop() {
		synchronized (queue) {
			if (queue.isEmpty()) {
				return new BlockDistort();
			} else {
				return queue.pop();
			}
		}
	}

	private void recycle( BlockDistort b ) {
		synchronized (queue) {
			queue.push(b);
		}
	}

	@Override
	protected void init( Input srcImg, Output dstImg ) {
		super.init(srcImg, dstImg);
		assigner.setImage(dstImg);
	}

	@Override
	public void applyAll() {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyAll(y0, y1);
			recycle(b);
		});
	}

	@Override
	public void applyAll( GrayU8 mask ) {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyAll(y0, y1, mask);
			recycle(b);
		});
	}

	@Override
	public void applyOnlyInside() {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyOnlyInside(y0, y1);
			recycle(b);
		});
	}

	@Override
	public void applyOnlyInside( GrayU8 mask ) {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyOnlyInside(y0, y1, mask);
			recycle(b);
		});
	}

	private int getNumberOfBands() {
		return srcImg.numBands;
	}

	@SuppressWarnings({"NullAway.Init"})
	private class BlockDistort {
		Point2D_F32 distorted = new Point2D_F32();
		PixelTransform<Point2D_F32> dstToSrc;
		InterpolatePixelMB<Input> interp = ImageDistortBasic_IL_MT.this.interp.copy();
		float[] values = new float[getNumberOfBands()];

		public void init() {
			dstToSrc = ImageDistortBasic_IL_MT.this.dstToSrc.copyConcurrent();
			interp.setImage(srcImg);
		}

		void applyAll( int y0, int y1 ) {
			init();

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

		void applyAll( int y0, int y1, GrayU8 mask ) {
			init();

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

		void applyOnlyInside( int y0, int y1 ) {
			init();

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

		void applyOnlyInside( int y0, int y1, GrayU8 mask ) {
			init();

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
}
