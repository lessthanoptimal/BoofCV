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
import boofcv.struct.image.*;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;
import lombok.Getter;
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundMovingBasic} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingBasic_IL<T extends ImageInterleaved<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundMovingBasic<T, Motion> {
	/** Background model. Pixels which haven't been assigned yet are marked with {@link Float#MAX_VALUE}. */
	@Getter protected InterleavedF32 background;
	// interpolates the input image
	protected InterpolatePixelMB<T> _interpolationInput;
	// interpolates the background image
	protected InterpolatePixelMB<InterleavedF32> _interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;

	protected GrowArray<Helper> helpers;
	protected Helper helper;

	public BackgroundMovingBasic_IL( float learnRate, float threshold,
									 Point2Transform2Model_F32<Motion> transform,
									 InterpolationType interpType,
									 ImageType<T> imageType ) {
		super(learnRate, threshold, transform, imageType);

		this._interpolationInput = FactoryInterpolation.createPixelMB(0, 255, interpType, BorderType.EXTENDED, imageType);

		int numBands = imageType.getNumBands();
		background = new InterleavedF32(1, 1, numBands);

		this._interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.il(numBands, InterleavedF32.class));
		this._interpolationBG.setImage(background);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		helpers = new GrowArray<>(() -> new Helper(imageType.numBands));
		helper = helpers.grow();
	}

	@Override public void initialize( int backgroundWidth, int backgroundHeight, Motion homeToWorld ) {
		background.reshape(backgroundWidth, backgroundHeight);
		GImageMiscOps.fill(background, Float.MAX_VALUE);

		this.homeToWorld.setTo(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override public void reset() {
		GImageMiscOps.fill(background, Float.MAX_VALUE);
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

			final int numBands = frame.getNumBands();
			final float minusLearn = 1.0f - learnRate;

			transform.setModel(worldToCurrent);
			for (int y = y0; y < y1; y++) {
				int indexBG = background.startIndex + y*background.stride + x0*numBands;
				for (int x = x0; x < x1; x++) {
					transform.compute(x, y, pixel);

					if (pixel.x >= 0 && pixel.x < frame.width && pixel.y >= 0 && pixel.y < frame.height) {
						interpolationInput.get(pixel.x, pixel.y, valueInput);

						for (int band = 0; band < numBands; band++, indexBG++) {

							float value = valueInput[band];
							float bg = background.data[indexBG];

							if (bg == Float.MAX_VALUE) {
								background.data[indexBG] = value;
							} else {
								background.data[indexBG] = minusLearn*bg + learnRate*value;
							}
						}
					} else {
						indexBG += numBands;
					}
				}
			}
		}

		protected void segment( int y0, int y1, Motion currentToWorld, T frame, GrayU8 segmented ) {
			final int numBands = background.getNumBands();

			final float thresholdSq = numBands*threshold*threshold;

			transform.setModel(currentToWorld);
			for (int y = y0; y < y1; y++) {
				int indexFrame = frame.startIndex + y*frame.stride;
				int indexSegmented = segmented.startIndex + y*segmented.stride;

				for (int x = 0; x < frame.width; x++, indexFrame += numBands, indexSegmented++) {
					transform.compute(x, y, pixel);

					escapeIf:
					if (pixel.x >= 0 && pixel.x < background.width && pixel.y >= 0 && pixel.y < background.height) {
						interpolationBG.get(pixel.x, pixel.y, valueBG);

						double sumErrorSq = 0;
						for (int band = 0; band < numBands; band++) {
							float bg = valueBG[band];
							float pixelFrame = inputWrapper.getF(indexFrame + band);

							if (bg == Float.MAX_VALUE) {
								segmented.data[indexSegmented] = unknownValue;
								break escapeIf;
							} else {
								float diff = bg - pixelFrame;
								sumErrorSq += diff*diff;
							}
						}

						segmented.data[indexSegmented] = (byte)(sumErrorSq <= thresholdSq ? 0 : 1);
					} else {
						// there is no background here. Just mark it as not moving to avoid false positives
						segmented.data[indexSegmented] = unknownValue;
					}
				}
			}
		}
	}
}
