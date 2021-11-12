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
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundMovingGmm} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGmm_MB<T extends ImageMultiBand<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGmm<T, Motion> {
	public BackgroundMovingGmm_MB( float learningPeriod, float decayCoef, int maxGaussians,
								   Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType ) {
		super(learningPeriod, decayCoef, maxGaussians, transformImageType, imageType);
	}

	@Override protected void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
		common.inputWrapperMB.wrap(frame);
		transformOG.setModel(worldToCurrent);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(y0, y1, 20, workspaceValues, (values, idx0, idx1) -> {
		final int idx0 = y0, idx1 = y1;
		final Point2D_F32 pixel =  values.pixel;
		final float[] valueInput = values.valueInput;
		values.transform.setModel(currentToWorld);
		for (int y = idx0; y < idx1; y++) {
			float[] modelRow = common.model.data[y];
			for (int x = x0; x < x1; x++) {
				int indexModel = x*common.modelStride;

				values.transform.compute(x, y, pixel);
				int xx = (int)(pixel.x + 0.5f);
				int yy = (int)(pixel.y + 0.5f);

				if (pixel.x >= 0 && xx < frame.width && pixel.y >= 0 && yy < frame.height) {
					common.inputWrapperMB.get(xx, yy, valueInput);

					common.updateMixture(valueInput, modelRow, indexModel); // TODO assigned mask here
				}
			}
		}
		//CONCURRENT_ABOVE }});
	}

	@Override protected void _segment( Motion currentToWorld, T frame, GrayU8 segmented ) {
		common.inputWrapperMB.wrap(frame);
		common.unknownValue = unknownValue;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, workspaceValues, (values, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		final float[] valueInput = values.valueInput;
		final Point2D_F32 pixel =  values.pixel;
		values.transform.setModel(currentToWorld);
		for (int y = idx0; y < idx1; y++) {
			int indexOut = segmented.startIndex + y*segmented.stride;
			for (int x = 0; x < frame.width; x++, indexOut++) {
				values.transform.compute(x, y, pixel);

				int xx = (int)(pixel.x + 0.5f);
				int yy = (int)(pixel.y + 0.5f);

				if (pixel.x >= 0 && xx < backgroundWidth && pixel.y >= 0 && yy < backgroundHeight) {
					common.inputWrapperMB.get(x, y, valueInput);

					float[] modelRow = common.model.data[yy];
					int indexModel = xx*common.modelStride;

					segmented.data[indexOut] = (byte)common.checkBackground(valueInput, modelRow, indexModel);
				} else {
					// there is no background here. Just mark it as not moving to avoid false positives
					segmented.data[indexOut] = unknownValue;
				}
			}
		}
		//CONCURRENT_ABOVE }});
	}
}
