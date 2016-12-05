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
import boofcv.struct.image.*;
import georegression.struct.InvertibleTransform;

/**
 * Implementation of {@link BackgroundMovingBasic} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingBasic_IL<T extends ImageInterleaved, Motion extends InvertibleTransform<Motion>>
	extends BackgroundMovingBasic<T,Motion>
{
	// where the background image is stored
	protected InterleavedF32 background;
	// interpolates the input image
	protected InterpolatePixelMB<T> interpolationInput;
	// interpolates the background image
	protected InterpolatePixelMB<InterleavedF32> interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;
	// storage for multi-band pixel values
	protected float[] pixelInput;
	protected float[] pixelBack;

	public BackgroundMovingBasic_IL(float learnRate, float threshold,
									Point2Transform2Model_F32<Motion> transform,
									InterpolationType interpType,
									ImageType<T> imageType) {
		super(learnRate, threshold,transform, imageType);

		this.interpolationInput = FactoryInterpolation.createPixelMB(0, 255, interpType,BorderType.EXTENDED,imageType);

		int numBands = imageType.getNumBands();
		background = new InterleavedF32(1,1,numBands);

		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.il(numBands, InterleavedF32.class));
		this.interpolationBG.setImage(background);

		pixelInput = new float[numBands];
		pixelBack = new float[numBands];

		inputWrapper = FactoryGImageMultiBand.create(imageType);
	}

	/**
	 * Returns the background image.  Pixels which haven't been assigned yet are marked with {@link Float#MAX_VALUE}.
	 *
	 * @return background image.
	 */
	public InterleavedF32 getBackground() {
		return background;
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {
		background.reshape(backgroundWidth,backgroundHeight);
		GImageMiscOps.fill(background, Float.MAX_VALUE);

		this.homeToWorld.set(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}

	@Override
	public void reset() {
		GImageMiscOps.fill(background,Float.MAX_VALUE);
	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, T frame) {

		transform.setModel(worldToCurrent);
		interpolationInput.setImage(frame);

		final int numBands = frame.getNumBands();
		float minusLearn = 1.0f - learnRate;

		for (int y = y0; y < y1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0*numBands;
			for (int x = x0; x < x1; x++ ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < frame.width && work.y >= 0 && work.y < frame.height) {

					interpolationInput.get(work.x, work.y, pixelInput);

					for (int band = 0; band < numBands; band++, indexBG++) {

						float value = pixelInput[band];
						float bg = background.data[indexBG];

						if( bg == Float.MAX_VALUE ) {
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

	@Override
	protected void _segment(Motion currentToWorld, T frame, GrayU8 segmented) {
		transform.setModel(currentToWorld);
		inputWrapper.wrap(frame);

		int numBands = background.getNumBands();

		float thresholdSq = numBands*threshold*threshold;

		for (int y = 0; y < frame.height; y++) {
			int indexFrame = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			for (int x = 0; x < frame.width; x++, indexFrame += numBands , indexSegmented++ ) {
				transform.compute(x,y,work);

				escapeIf:
				if( work.x >= 0 && work.x < background.width && work.y >= 0 && work.y < background.height) {

					interpolationBG.get(work.x,work.y,pixelBack);

					double sumErrorSq = 0;
					for (int band = 0; band < numBands; band++) {
						float bg = pixelBack[band];
						float pixelFrame = inputWrapper.getF(indexFrame + band);

						if( bg == Float.MAX_VALUE ) {
							segmented.data[indexSegmented] = unknownValue;
							break escapeIf;
						} else {
							float diff = bg - pixelFrame;
							sumErrorSq += diff*diff;
						}
					}

					if ( sumErrorSq <= thresholdSq) {
						segmented.data[indexSegmented] = 0;
					} else {
						segmented.data[indexSegmented] = 1;
					}
				} else {
					// there is no background here.  Just mark it as not moving to avoid false positives
					segmented.data[indexSegmented] = unknownValue;
				}
			}
		}
	}


}
