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
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.*;
import georegression.struct.InvertibleTransform;

/**
 * Implementation of {@link BackgroundMovingGaussian} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGaussian_SB <T extends ImageGray, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGaussian<T,Motion>
{

	// interpolates the input image
	protected InterpolatePixelS<T> interpolateInput;
	// interpolates the background image
	protected InterpolatePixelMB<Planar<GrayF32>> interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageGray inputWrapper;
	// storage for multi-band pixel values
	protected float[] pixelBG = new float[2];

	// background is composed of two channels.  0 = mean, 1 = variance
	Planar<GrayF32> background = new Planar<>(GrayF32.class,1,1,2);

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated.  0 = static  1.0 = instant.  Try 0.05
	 * @param threshold Threshold for background.  Try 10.
	 * @param transform Used to apply motion model
	 * @param interpType Type of interpolation.  BILINEAR recommended for accuracy. NEAREST_NEIGHBOR for speed. .
	 * @param imageType Type of input image.
	 */
	public BackgroundMovingGaussian_SB(float learnRate, float threshold,
									   Point2Transform2Model_F32<Motion> transform,
									   InterpolationType interpType,
									   Class<T> imageType)
	{
		super(learnRate, threshold, transform, ImageType.single(imageType));

		this.interpolateInput = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.pl(2, GrayF32.class));
		this.interpolationBG.setImage(background);
		inputWrapper = FactoryGImageGray.create(imageType);
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {
		background.reshape(backgroundWidth,backgroundHeight);
		GImageMiscOps.fill(background.getBand(0),0);
		GImageMiscOps.fill(background.getBand(1),-1);

		this.homeToWorld.set(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override
	public void reset() {
		GImageMiscOps.fill(background.getBand(0),0);
		GImageMiscOps.fill(background.getBand(1),-1);
	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, T frame) {
		transform.setModel(worldToCurrent);
		interpolateInput.setImage(frame);

		float minusLearn = 1.0f - learnRate;

		GrayF32 backgroundMean = background.getBand(0);
		GrayF32 backgroundVar = background.getBand(1);

		for (int y = y0; y < y1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0;
			for (int x = x0; x < x1; x++, indexBG++ ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < frame.width && work.y >= 0 && work.y < frame.height) {
					float inputValue = interpolateInput.get(work.x,work.y);
					float meanBG = backgroundMean.data[indexBG];
					float varianceBG = backgroundVar.data[indexBG];

					if( varianceBG < 0) {
						backgroundMean.data[indexBG] = inputValue;
						backgroundVar.data[indexBG] = initialVariance;
					} else {
						float diff = meanBG-inputValue;
						backgroundMean.data[indexBG] = minusLearn*meanBG + learnRate*inputValue;
						backgroundVar.data[indexBG] = minusLearn*varianceBG + learnRate*diff*diff;
					}
				}
			}
		}
	}

	@Override
	protected void _segment(Motion currentToWorld, T frame, GrayU8 segmented) {
		transform.setModel(currentToWorld);
		inputWrapper.wrap(frame);

		for (int y = 0; y < frame.height; y++) {
			int indexFrame = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			for (int x = 0; x < frame.width; x++, indexFrame++ , indexSegmented++ ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < background.width && work.y >= 0 && work.y < background.height) {
					interpolationBG.get(work.x,work.y,pixelBG);
					float pixelFrame = inputWrapper.getF(indexFrame);

					float meanBG = pixelBG[0];
					float varBG = pixelBG[1];

					if( varBG < 0) {
						segmented.data[indexSegmented] = unknownValue;
					} else {
						float diff = meanBG - pixelFrame;
						float chisq = diff*diff/varBG;

						if (chisq <= threshold) {
							segmented.data[indexSegmented] = 0;
						} else {
							if( diff > minimumDifference || -diff > minimumDifference )
								segmented.data[indexSegmented] = 1;
							else
								segmented.data[indexSegmented] = 0;
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
