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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.*;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundMovingGaussian} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGaussian_SB<T extends ImageGray<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGaussian<T, Motion> {

	// interpolates the input image
	protected InterpolatePixelS<T> interpolateInput;
	// interpolates the background image
	protected InterpolatePixelMB<Planar<GrayF32>> interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageGray inputWrapper;
	// storage for multi-band pixel values
	protected float[] pixelBG = new float[2];

	// background is composed of two channels. 0 = mean, 1 = variance
	Planar<GrayF32> background = new Planar<>(GrayF32.class, 1, 1, 2);

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated. 0 = static  1.0 = instant. Try 0.05
	 * @param threshold Threshold for background. Try 10.
	 * @param transform Used to apply motion model
	 * @param interpType Type of interpolation. BILINEAR recommended for accuracy. NEAREST_NEIGHBOR for speed. .
	 * @param imageType Type of input image.
	 */
	public BackgroundMovingGaussian_SB( float learnRate, float threshold,
										Point2Transform2Model_F32<Motion> transform,
										InterpolationType interpType,
										Class<T> imageType ) {
		super(learnRate, threshold, transform, ImageType.single(imageType));

		this.interpolateInput = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.pl(2, GrayF32.class));
		this.interpolationBG.setImage(background);
		inputWrapper = FactoryGImageGray.create(imageType);
	}

	@Override public void initialize( int backgroundWidth, int backgroundHeight, Motion homeToWorld ) {
		background.reshape(backgroundWidth, backgroundHeight);
		GImageMiscOps.fill(background.getBand(0), 0);
		GImageMiscOps.fill(background.getBand(1), -1);

		this.homeToWorld.setTo(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override public void reset() {
		GImageMiscOps.fill(background.getBand(0), 0);
		GImageMiscOps.fill(background.getBand(1), -1);
	}

	@Override protected void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
		interpolateInput.setImage(frame);

		final float minusLearn = 1.0f - learnRate;

		GrayF32 backgroundMean = background.getBand(0);
		GrayF32 backgroundVar = background.getBand(1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(y0, y1, 20, workspaceValues, (values, idx0, idx1) -> {
		final int idx0 = y0, idx1 = y1;
		final Point2D_F32 pixel =  values.pixel;
		values.transform.setModel(currentToWorld);
		for (int y = idx0; y < idx1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0;
			for (int x = x0; x < x1; x++, indexBG++) {
				values.transform.compute(x, y, pixel);

				if (pixel.x >= 0 && pixel.x < frame.width && pixel.y >= 0 && pixel.y < frame.height) {
					float inputValue = interpolateInput.get(pixel.x, pixel.y);
					float meanBG = backgroundMean.data[indexBG];
					float varianceBG = backgroundVar.data[indexBG];

					if (varianceBG < 0) {
						backgroundMean.data[indexBG] = inputValue;
						backgroundVar.data[indexBG] = initialVariance;
					} else {
						float diff = meanBG - inputValue;
						backgroundMean.data[indexBG] = minusLearn*meanBG + learnRate*inputValue;
						backgroundVar.data[indexBG] = minusLearn*varianceBG + learnRate*diff*diff;
					}
				}
			}
		}
		//CONCURRENT_ABOVE }});
	}

	@Override protected void _segment( Motion currentToWorld, T frame, GrayU8 segmented ) {
		inputWrapper.wrap(frame);

		final float minimumDifferenceSq = minimumDifference*minimumDifference;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, workspaceValues, (values, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		final Point2D_F32 pixel =  values.pixel;
		values.transform.setModel(currentToWorld);
		for (int y = idx0; y < idx1; y++) {
			int indexFrame = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			for (int x = 0; x < frame.width; x++, indexFrame++, indexSegmented++) {
				values.transform.compute(x, y, pixel);

				if (pixel.x >= 0 && pixel.x < background.width && pixel.y >= 0 && pixel.y < background.height) {
					interpolationBG.get(pixel.x, pixel.y, pixelBG);
					float pixelFrame = inputWrapper.getF(indexFrame);

					float meanBG = pixelBG[0];
					float varBG = pixelBG[1];

					if (varBG < 0) {
						segmented.data[indexSegmented] = unknownValue;
					} else {
						float diff = meanBG - pixelFrame;
						float chisq = diff*diff/varBG;

						if (chisq <= threshold) {
							segmented.data[indexSegmented] = 0;
						} else {
							segmented.data[indexSegmented] = (byte)(diff*diff > minimumDifferenceSq ? 1 : 0);
						}
					}
				} else {
					// there is no background here. Just mark it as not moving to avoid false positives
					segmented.data[indexSegmented] = unknownValue;
				}
			}
		}
		//CONCURRENT_ABOVE }});
	}
}
