/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

/**
 * Implementation of {@link BackgroundMovingGmm} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGmm_MB<T extends ImageMultiBand<T>, Motion extends InvertibleTransform<Motion>>
	extends BackgroundMovingGmm<T,Motion>
{
	public BackgroundMovingGmm_MB(float learningPeriod, float decayCoef, int maxGaussians,
								  Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType)
	{
		super(learningPeriod, decayCoef, maxGaussians, transformImageType, imageType);
	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, T frame) {

		common.inputWrapperMB.wrap(frame);
		transform.setModel(worldToCurrent);

		for (int y = y0; y < y1; y++) {
			float modelRow[] = common.model.data[y];
			for (int x = x0; x < x1; x++) {
				int indexModel = x*common.modelStride;

				transform.compute(x,y,work);
				int xx = (int)(work.x+0.5f);
				int yy = (int)(work.y+0.5f);

				if( work.x >= 0 && xx < frame.width && work.y >= 0 && yy < frame.height) {

					common.inputWrapperMB.get(xx,yy,common.inputPixel);

					common.updateMixture(common.inputPixel,modelRow,indexModel); // TODO assigned mask here
				}
			}
		}
	}

	@Override
	protected void _segment(Motion currentToWorld, T frame, GrayU8 segmented) {
		common.inputWrapperMB.wrap(frame);
		transform.setModel(currentToWorld);
		common.unknownValue = unknownValue;

		for (int y = 0; y < frame.height; y++) {
			int indexOut = segmented.startIndex + y*segmented.stride;
			for (int x = 0; x < frame.width; x++, indexOut++) {

				transform.compute(x,y,work);

				int xx = (int)(work.x+0.5f);
				int yy = (int)(work.y+0.5f);

				if( work.x >= 0 && xx < backgroundWidth && work.y >= 0 && yy < backgroundHeight) {

					common.inputWrapperMB.get(x,y,common.inputPixel);

					float modelRow[] = common.model.data[yy];
					int indexModel = xx*common.modelStride;

					segmented.data[indexOut] = (byte)common.checkBackground(common.inputPixel, modelRow, indexModel);
				}else {
					// there is no background here.  Just mark it as not moving to avoid false positives
					segmented.data[indexOut] = unknownValue;
				}
			}
		}
	}
}
