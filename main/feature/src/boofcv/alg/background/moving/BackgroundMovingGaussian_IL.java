/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import georegression.struct.InvertibleTransform;

/**
 * Implementation of {@link BackgroundMovingGaussian} for {@link ImageInterleaved}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGaussian_IL<T extends ImageInterleaved, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGaussian<T,Motion>
{

	// interpolates the input image
	protected InterpolatePixelMB<T> interpolateInput;
	// interpolates the background image
	protected InterpolatePixelMB<InterleavedF32> interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;
	// storage for multi-band pixel values
	protected float[] pixelBG;
	protected float[] pixelInput;

	// background is composed of bands*2 channels.  even = mean, odd = variance
	InterleavedF32 background;

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated.  0 = static  1.0 = instant.  Try 0.05
	 * @param threshold Threshold for background.  Consult a chi-square table for reasonably values.
	 *                  10 to 16 for 1 to 3 bands.
	 * @param transform Used to apply motion model
	 * @param interpType Type of interpolation.  BILINEAR recommended for accuracy. NEAREST_NEIGHBOR for speed. .
	 * @param imageType Type of input image.
	 */
	public BackgroundMovingGaussian_IL(float learnRate, float threshold,
									   Point2Transform2Model_F32<Motion> transform,
									   InterpolationType interpType,
									   ImageType<T> imageType)
	{
		super(learnRate, threshold, transform, imageType);

		int numBands = imageType.getNumBands();

		this.interpolateInput = FactoryInterpolation.createPixelMB(0, 255,
				InterpolationType.BILINEAR, BorderType.EXTENDED, imageType);

		background = new InterleavedF32(1,1,2*numBands);
		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.il(numBands*2, InterleavedF32.class));
		this.interpolationBG.setImage(background);
		inputWrapper = FactoryGImageMultiBand.create(imageType);

		pixelBG = new float[2*numBands];
		pixelInput = new float[numBands];
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {
		background.reshape(backgroundWidth,backgroundHeight);
		GImageMiscOps.fill(background, -1);

		this.homeToWorld.set(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override
	public void reset() {
		GImageMiscOps.fill(background, -1);
	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, T frame) {
		transform.setModel(worldToCurrent);
		interpolateInput.setImage(frame);

		float minusLearn = 1.0f - learnRate;

		final int numBands = background.getNumBands()/2;

		for (int y = y0; y < y1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0*background.numBands;
			for (int x = x0; x < x1; x++, indexBG += numBands*2 ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < frame.width && work.y >= 0 && work.y < frame.height) {
					interpolateInput.get(work.x,work.y,pixelInput);

					for (int band = 0; band < numBands; band++) {
						int indexBG_band = indexBG + band*2;

						float inputValue = pixelInput[band];
						float meanBG     = background.data[indexBG_band];
						float varianceBG = background.data[indexBG_band + 1];

						if( varianceBG < 0) {
							background.data[indexBG_band]   = inputValue;
							background.data[indexBG_band+1] = initialVariance;
						} else {
							float diff = meanBG-inputValue;
							background.data[indexBG_band]   = minusLearn*meanBG + learnRate*inputValue;
							background.data[indexBG_band+1] = minusLearn*varianceBG + learnRate*diff*diff;
						}
					}
				}
			}
		}
	}

	@Override
	protected void _segment(Motion currentToWorld, T frame, GrayU8 segmented) {
		transform.setModel(currentToWorld);
		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands()/2;
		float adjustedMinimumDifference = minimumDifference*numBands;

		for (int y = 0; y < frame.height; y++) {
			int indexFrame = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			for (int x = 0; x < frame.width; x++, indexFrame += numBands , indexSegmented++ ) {
				transform.compute(x,y,work);

				escapeIf:if( work.x >= 0 && work.x < background.width && work.y >= 0 && work.y < background.height) {
					interpolationBG.get(work.x,work.y,pixelBG);
					inputWrapper.getF(indexFrame,pixelInput);

					float mahalanobis = 0;

					for (int band = 0; band < numBands; band++) {
						float meanBG = pixelBG[band*2];
						float varBG = pixelBG[band*2+1];

						if (varBG < 0) {
							segmented.data[indexSegmented] = unknownValue;
							break escapeIf;
						} else {
							float diff = meanBG - pixelInput[band];
							mahalanobis += diff * diff / varBG;
						}
					}

					if (mahalanobis <= threshold) {
						segmented.data[indexSegmented] = 0;
					} else {
						if( minimumDifference > 0 ) {
							float sumAbsDiff = 0;
							for (int band = 0; band < numBands; band++) {
								sumAbsDiff += Math.abs(pixelBG[band * 2] - pixelInput[band]);
							}
							if (sumAbsDiff >= adjustedMinimumDifference) {
								segmented.data[indexSegmented] = 1;
							} else {
								segmented.data[indexSegmented] = 0;
							}
						} else {
							segmented.data[indexSegmented] = 1;
						}
					}
				} else {
					// there is no background here.  Just mark it as not moving to avoid false positives
					segmented.data[indexSegmented] = unknownValue;
				}
			}
		}
	}
}
