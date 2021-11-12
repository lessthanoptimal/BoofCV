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

package boofcv.alg.background.moving;

import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundMovingGmm} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGmm_SB<T extends ImageGray<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGmm<T, Motion> {

	protected GrowArray<Helper> helpers;
	protected Helper helper;

	public BackgroundMovingGmm_SB( float learningPeriod, float decayCoef, int maxGaussians,
								   Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType ) {
		super(learningPeriod, decayCoef, maxGaussians, transformImageType, imageType);

		helpers = new GrowArray<>(Helper::new);
		helper = helpers.grow();
	}

	@Override protected void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
		common.inputWrapperG.wrap(frame);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(y0, y1, 20, helpers, (helper, idx0, idx1) -> {
		final int idx0 = y0, idx1 = y1;
		helper.updateBackground(x0, idx0, x1, idx1, frame);
		//CONCURRENT_INLINE });
	}

	@Override protected void _segment( Motion currentToWorld, T frame, GrayU8 segmented ) {
		common.inputWrapperG.wrap(frame);
		common.unknownValue = unknownValue;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, helpers, (helper, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		helper.segment(idx0, idx1, currentToWorld, frame, segmented);
		//CONCURRENT_INLINE });
	}

	private class Helper {
		final private Point2D_F32 pixel = new Point2D_F32();
		final private Point2Transform2Model_F32<Motion> transform;

		public Helper() {
			transform = (Point2Transform2Model_F32<Motion>)_transform.copyConcurrent();
		}

		public void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
			transform.setModel(worldToCurrent);
			for (int y = y0; y < y1; y++) {
				float[] modelRow = common.model.data[y];
				for (int x = x0; x < x1; x++) {
					int indexModel = x*common.modelStride;

					transform.compute(x, y, pixel);
					final int xx = (int)(pixel.x + 0.5f);
					final int yy = (int)(pixel.y + 0.5f);

					if (pixel.x >= 0 && xx < frame.width && pixel.y >= 0 && yy < frame.height) {
						float pixelValue = common.inputWrapperG.unsafe_getF(xx, yy);
						common.updateMixture(pixelValue, modelRow, indexModel); // TODO assigned mask here
					}
				}
			}
		}

		protected void segment( int y0, int y1, Motion currentToWorld, T frame, GrayU8 segmented ) {
			transform.setModel(currentToWorld);
			for (int y = y0; y < y1; y++) {
				int indexOut = segmented.startIndex + y*segmented.stride;
				for (int x = 0; x < frame.width; x++, indexOut++) {

					transform.compute(x, y, pixel);

					final int xx = (int)(pixel.x + 0.5f);
					final int yy = (int)(pixel.y + 0.5f);

					if (pixel.x >= 0 && xx < backgroundWidth && pixel.y >= 0 && yy < backgroundHeight) {
						float pixelValue = common.inputWrapperG.unsafe_getF(x, y);

						float[] modelRow = common.model.data[yy];
						int indexModel = xx*common.modelStride;

						segmented.data[indexOut] = (byte)common.checkBackground(pixelValue, modelRow, indexModel);
					} else {
						// there is no background here. Just mark it as not moving to avoid false positives
						segmented.data[indexOut] = unknownValue;
					}
				}
			}
		}
	}
}
