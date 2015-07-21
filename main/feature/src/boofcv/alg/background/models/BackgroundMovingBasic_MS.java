/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.models;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PointTransformModel_F32;
import boofcv.struct.image.*;
import georegression.struct.InvertibleTransform;

/**
 * Implementation of {@link BackgroundMovingBasic} for {@link MultiSpectral}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingBasic_MS<T extends ImageSingleBand, Motion extends InvertibleTransform<Motion>>
	extends BackgroundMovingBasic<MultiSpectral<T>,Motion>
{
	MultiSpectral<ImageFloat32> background;
	InterpolatePixelS<T> interpolation;
	InterpolatePixelMB<MultiSpectral<ImageFloat32>> interpolationBG;

	public BackgroundMovingBasic_MS(float learnRate, float threshold,
									PointTransformModel_F32<Motion> transform,
									TypeInterpolate interpType,
									ImageType<MultiSpectral<T>> imageType) {
		super(learnRate, threshold,transform, imageType);

		Class<T> type = imageType.getImageClass();
		this.interpolation = FactoryInterpolation.bilinearPixelS(type, BorderType.EXTENDED);

		int numBands = imageType.getNumBands();
		background = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,numBands);

		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.ms(numBands, ImageFloat32.class));
	}

	/**
	 * Returns the background image.  Pixels which haven't been assigned yet are marked with {@link Float#MAX_VALUE}.
	 *
	 * @return background image.
	 */
	public MultiSpectral<ImageFloat32> getBackground() {
		return background;
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {
		background.reshape(backgroundWidth,backgroundHeight);
		GImageMiscOps.fill(background, Float.MAX_VALUE);
	}

	@Override
	public void reset() {
		GImageMiscOps.fill(background,Float.MAX_VALUE);
	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, MultiSpectral<T> frame) {

		transform.setModel(worldToCurrent);

		final int numBands = frame.getNumBands();
		float minusLearn = 1.0f - learnRate;

		for (int y = y0; y < y1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0;
			for (int x = x0; x < x1; x++, indexBG++ ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < frame.width && work.y >= 0 && work.y < frame.height) {

					for (int band = 0; band < numBands; band++) {
						interpolation.setImage(frame.getBand(band));
						ImageFloat32 backgroundBand = background.getBand(band);

						float value = interpolation.get(work.x,work.y);
						float bg = backgroundBand.data[indexBG];

						if( bg == Float.MAX_VALUE ) {
							backgroundBand.data[indexBG] = value;
						} else {
							backgroundBand.data[indexBG] = minusLearn*value + learnRate*bg;
						}
					}
				}
			}
		}
	}

	@Override
	protected void _segment(Motion currentToWorld, MultiSpectral<T> frame, ImageUInt8 segmented) {
//		transform.setModel(worldToCurrent);
//
//		float thresholdSq = threshold*threshold;
//
//		int numBands = background.getNumBands();
//
//		for (int y = 0; y < frame.height; y++) {
//			int indexFrame = frame.startIndex + y*frame.stride;
//			int indexSegmented = segmented.startIndex + y*segmented.stride;
//
//			for (int x = 0; x < frame.width; x++, indexFrame++ , indexSegmented++ ) {
//				transform.compute(x,y,work);
//
//				if( work.x >= 0 && work.x < background.width && work.y >= 0 && work.y < background.height) {
//
//					double errorSq = 0;
//					for (int band = 0; band < numBands; band++) {
//						interpolationBG.setImage(background.getBand(band));
//					}
//
//					float bg = interpolation.get(work.x,work.y);
//					float pixelFrame = inputImageWrapper.getF(indexFrame);
//
//					if( bg == Float.MAX_VALUE ) {
//						segmented.data[indexSegmented] = 0;
//					} else {
//						float diff = bg - pixelFrame;
//						if (diff * diff <= thresholdSq) {
//							segmented.data[indexSegmented] = 0;
//						} else {
//							segmented.data[indexSegmented] = 1;
//						}
//					}
//				}
//			}
//		}
	}


}
