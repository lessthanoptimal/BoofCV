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
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundMovingGaussian} for {@link ImageInterleaved}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGaussian_IL<T extends ImageInterleaved<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingGaussian<T, Motion> {
	// interpolates the input image
	protected final InterpolatePixelMB<T> _interpolationInput;
	// interpolates the background image
	protected final InterpolatePixelMB<InterleavedF32> _interpolationBG;

	// wrappers which provide abstraction across image types
	protected final GImageMultiBand inputWrapper;

	// background is composed of bands*2 channels. even = mean, odd = variance
	protected final InterleavedF32 background;

	protected final GrowArray<Helper> helpers;
	protected final Helper helper;

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated. 0 = static  1.0 = instant. Try 0.05
	 * @param threshold Threshold for background. Consult a chi-square table for reasonably values.
	 * 10 to 16 for 1 to 3 bands.
	 * @param transform Used to apply motion model
	 * @param interpType Type of interpolation. BILINEAR recommended for accuracy. NEAREST_NEIGHBOR for speed. .
	 * @param imageType Type of input image.
	 */
	public BackgroundMovingGaussian_IL( float learnRate, float threshold,
										Point2Transform2Model_F32<Motion> transform,
										InterpolationType interpType,
										ImageType<T> imageType ) {
		super(learnRate, threshold, transform, imageType);

		int numBands = imageType.getNumBands();

		this._interpolationInput = FactoryInterpolation.createPixelMB(0, 255,
				InterpolationType.BILINEAR, BorderType.EXTENDED, imageType);

		background = new InterleavedF32(1, 1, 2*numBands);
		this._interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.il(numBands*2, InterleavedF32.class));
		this._interpolationBG.setImage(background);
		inputWrapper = FactoryGImageMultiBand.create(imageType);

		helpers = new GrowArray<>(() -> new Helper(imageType.numBands));
		helper = helpers.grow();
	}

	@Override public void initialize( int backgroundWidth, int backgroundHeight, Motion homeToWorld ) {
		background.reshape(backgroundWidth, backgroundHeight);
		GImageMiscOps.fill(background, -1);

		this.homeToWorld.setTo(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override public void reset() {
		GImageMiscOps.fill(background, -1);
	}

	@Override protected void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(y0, y1, 20, helpers, (helper, idx0, idx1) -> {
		final int idx0 = y0, idx1 = y1;
		helper.updateBackground(x0, idx0, x1, idx1, frame);
		//CONCURRENT_INLINE });
	}

	@Override protected void _segment( Motion currentToWorld, T frame, GrayU8 segmented ) {
		inputWrapper.wrap(frame);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, helpers, (helper, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		helper.segment(idx0, idx1, currentToWorld, frame, segmented);
		//CONCURRENT_INLINE });
	}

	private class Helper {
		final private float[] valueInput;
		final private float[] valueBG;
		final private Point2D_F32 pixel = new Point2D_F32();
		final private Point2Transform2Model_F32<Motion> transform;
		final private InterpolatePixelMB<T> interpolationInput;
		final private InterpolatePixelMB<InterleavedF32> interpolationBG;

		public Helper( int numBands ) {
			valueInput = new float[numBands];
			valueBG = new float[2*numBands];
			transform = (Point2Transform2Model_F32<Motion>)_transform.copyConcurrent();
			interpolationInput = _interpolationInput.copy();
			interpolationBG = _interpolationBG.copy();
			interpolationBG.setImage(background);
		}

		public void updateBackground( int x0, int y0, int x1, int y1, T frame ) {
			interpolationInput.setImage(frame);

			final float minusLearn = 1.0f - learnRate;

			final int numBands = background.getNumBands()/2;

			transform.setModel(worldToCurrent);
			for (int y = y0; y < y1; y++) {
				int indexBG = background.startIndex + y*background.stride + x0*background.numBands;
				for (int x = x0; x < x1; x++, indexBG += numBands*2) {
					transform.compute(x, y, pixel);

					if (!(pixel.x >= 0 && pixel.x < frame.width && pixel.y >= 0 && pixel.y < frame.height)) {
						continue;
					}
					interpolationInput.get(pixel.x, pixel.y, valueInput);

					for (int band = 0; band < numBands; band++) {
						int indexBG_band = indexBG + band*2;

						final float inputValue = valueInput[band];
						final float meanBG = background.data[indexBG_band];
						final float varianceBG = background.data[indexBG_band + 1];

						if (varianceBG < 0) {
							background.data[indexBG_band] = inputValue;
							background.data[indexBG_band + 1] = initialVariance;
						} else {
							float diff = meanBG - inputValue;
							background.data[indexBG_band] = minusLearn*meanBG + learnRate*inputValue;
							background.data[indexBG_band + 1] = minusLearn*varianceBG + learnRate*diff*diff;
						}
					}
				}
			}
		}

		protected void segment( int y0, int y1, Motion currentToWorld, T frame, GrayU8 segmented ) {
			final int numBands = background.getNumBands()/2;
			final float adjustedMinimumDifference = minimumDifference*numBands;

			transform.setModel(currentToWorld);
			for (int y = y0; y < y1; y++) {
				int indexFrame = frame.startIndex + y*frame.stride;
				int indexSegmented = segmented.startIndex + y*segmented.stride;

				for (int x = 0; x < frame.width; x++, indexFrame += numBands, indexSegmented++) {
					transform.compute(x, y, pixel);

					escapeIf:
					if (pixel.x >= 0 && pixel.x < background.width && pixel.y >= 0 && pixel.y < background.height) {
						interpolationBG.get(pixel.x, pixel.y, valueBG);
						inputWrapper.getF(indexFrame, valueInput);

						float mahalanobis = 0;

						for (int band = 0; band < numBands; band++) {
							final float meanBG = valueBG[band*2];
							final float varBG = valueBG[band*2 + 1];

							if (varBG < 0) {
								segmented.data[indexSegmented] = unknownValue;
								break escapeIf;
							} else {
								float diff = meanBG - valueInput[band];
								mahalanobis += diff*diff/varBG;
							}
						}

						if (mahalanobis <= threshold) {
							segmented.data[indexSegmented] = 0;
						} else {
							if (minimumDifference > 0) {
								float sumAbsDiff = 0;
								for (int band = 0; band < numBands; band++) {
									sumAbsDiff += Math.abs(valueBG[band*2] - valueInput[band]);
								}
								segmented.data[indexSegmented] = (byte)(sumAbsDiff >= adjustedMinimumDifference ? 1 : 0);
							} else {
								segmented.data[indexSegmented] = 1;
							}
						}
					} else {
						// there is no background here. Just mark it as not moving to avoid false positives
						segmented.data[indexSegmented] = unknownValue;
					}
				}
			}
		}
	}
}
