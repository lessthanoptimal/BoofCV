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
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;

import java.util.ArrayDeque;

/**
 * Except for very simple functions, computing the per pixel distortion is an expensive operation.
 * To overcome this problem the distortion is computed once and cached. Then when the image is distorted
 * again the save results are simply recalled and not computed again.
 *
 * @author Peter Abeles
 */
public class ImageDistortCache_SB_MT<Input extends ImageGray<Input>, Output extends ImageGray<Output>>
		extends ImageDistortCache_SB<Input, Output> {

	private final ArrayDeque<BlockDistort> queue = new ArrayDeque<>();

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 */
	public ImageDistortCache_SB_MT( AssignPixelValue_SB<Output> assigner,
									InterpolatePixelS<Input> interp ) {
		super(assigner, interp);
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
		if (dirty || width != dstImg.width || height != dstImg.height) {
			width = dstImg.width;
			height = dstImg.height;
			map = new Point2D_F32[width*height];
			for (int i = 0; i < map.length; i++) {
				map[i] = new Point2D_F32();
			}

			BoofConcurrency.loopBlocks(0, height, ( y0, y1 ) -> {
				PixelTransform<Point2D_F32> dstToSrc = this.dstToSrc.copyConcurrent();
				for (int y = y0; y < y1; y++) {
					int index = y*width;
					for (int x = 0; x < width; x++) {
						dstToSrc.compute(x, y, map[index++]);
					}
				}
			});
			dirty = false;
		} else if (dstImg.width != width || dstImg.height != height)
			throw new IllegalArgumentException("Unexpected dstImg dimension");

		this.srcImg = srcImg;
		this.dstImg = dstImg;
		interp.setImage(srcImg);
		assigner.setImage(dstImg);
	}

	@Override
	protected void renderAll() {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyAll(y0, y1);
			recycle(b);
		});
	}

	@Override
	protected void renderAll( GrayU8 mask ) {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyAll(y0, y1, mask);
			recycle(b);
		});
	}

	@Override
	protected void applyOnlyInside() {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyOnlyInside(y0, y1);
			recycle(b);
		});
	}

	@Override
	protected void applyOnlyInside( GrayU8 mask ) {
		BoofConcurrency.loopBlocks(y0, y1, ( y0, y1 ) -> {
			BlockDistort b = pop();
			b.applyOnlyInside(y0, y1, mask);
			recycle(b);
		});
	}

	private class BlockDistort {
		InterpolatePixelS<Input> interp = ImageDistortCache_SB_MT.this.interp.copy();

		public void init() {
			interp.setImage(srcImg);
		}

		void applyAll( int y0, int y1 ) {
			init();
			for (int y = y0; y < y1; y++) {
				int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
				for (int x = x0; x < x1; x++, indexDst++) {
					Point2D_F32 s = map[indexDst];

					assigner.assign(indexDst, interp.get(s.x, s.y));
				}
			}
		}

		void applyAll( int y0, int y1, GrayU8 mask ) {
			init();
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

		void applyOnlyInside( int y0, int y1 ) {
			init();

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

		void applyOnlyInside( int y0, int y1, GrayU8 mask ) {
			init();

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
	}
}
