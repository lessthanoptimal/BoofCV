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

package boofcv.alg.background.moving;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
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
	// where the background image is stored
	protected MultiSpectral<ImageFloat32> background;
	// interpolates the input image
	protected InterpolatePixelMB<MultiSpectral<T>> interpolationInput;
	// interpolates the background image
	protected InterpolatePixelMB<MultiSpectral<ImageFloat32>> interpolationBG;

	// wrappers which provide abstraction across image types
	protected GImageMultiBand backgroundWrapper;
	protected GImageMultiBand inputWrapper;
	// storage for multi-band pixel values
	protected float[] pixelInput;
	protected float[] pixelBack;

	public BackgroundMovingBasic_MS(float learnRate, float threshold,
									PointTransformModel_F32<Motion> transform,
									TypeInterpolate interpType,
									ImageType<MultiSpectral<T>> imageType) {
		super(learnRate, threshold,transform, imageType);

		this.interpolationInput = FactoryInterpolation.createPixelMB(0, 255, interpType,BorderType.EXTENDED,imageType);

		int numBands = imageType.getNumBands();
		background = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,numBands);

		this.interpolationBG = FactoryInterpolation.createPixelMB(
				0, 255, interpType, BorderType.EXTENDED, ImageType.ms(numBands, ImageFloat32.class));
		this.interpolationBG.setImage(background);

		pixelInput = new float[numBands];
		pixelBack = new float[numBands];

		backgroundWrapper = FactoryGImageMultiBand.create(ImageType.ms(numBands, ImageFloat32.class));
		backgroundWrapper.wrap(background);

		inputWrapper = FactoryGImageMultiBand.create(imageType);
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
	protected void updateBackground(int x0, int y0, int x1, int y1, MultiSpectral<T> frame) {

		transform.setModel(worldToCurrent);
		interpolationInput.setImage(frame);

		final int numBands = frame.getNumBands();
		float minusLearn = 1.0f - learnRate;

		for (int y = y0; y < y1; y++) {
			int indexBG = background.startIndex + y*background.stride + x0;
			for (int x = x0; x < x1; x++, indexBG++ ) {
				transform.compute(x,y,work);

				if( work.x >= 0 && work.x < frame.width && work.y >= 0 && work.y < frame.height) {

					interpolationInput.get(work.x,work.y, pixelInput);
					backgroundWrapper.getF(indexBG,pixelBack);

					for (int band = 0; band < numBands; band++) {

						float value = pixelInput[band];
						float bg = pixelBack[band];

						if( bg == Float.MAX_VALUE ) {
							pixelBack[band] = value;
						} else {
							pixelBack[band] = minusLearn*bg + learnRate*value;
						}
					}
					backgroundWrapper.setF(indexBG,pixelBack);
				}
			}
		}
	}

	@Override
	protected void _segment(Motion currentToWorld, MultiSpectral<T> frame, ImageUInt8 segmented) {
		transform.setModel(currentToWorld);
		inputWrapper.wrap(frame);

		int numBands = background.getNumBands();

		float thresholdSq = numBands*threshold*threshold;

		for (int y = 0; y < frame.height; y++) {
			int indexFrame = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			for (int x = 0; x < frame.width; x++, indexFrame++ , indexSegmented++ ) {
				transform.compute(x,y,work);

				escapeIf:
				if( work.x >= 0 && work.x < background.width && work.y >= 0 && work.y < background.height) {

					interpolationBG.get(work.x,work.y,pixelBack);
					inputWrapper.getF(indexFrame,pixelInput);

					double sumErrorSq = 0;
					for (int band = 0; band < numBands; band++) {
						float bg = pixelBack[band];
						float pixelFrame = pixelInput[band];

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
