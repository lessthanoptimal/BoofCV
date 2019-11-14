/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageNormalization;
import boofcv.alg.misc.NormalizeParameters;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link StereoDisparity} that will convert all inputs to float and normalize the input to have
 * zero mean and values between 0 and 1.
 *
 * @author Peter Abeles
 */
public class DisparityBlockMatchCorrelation<T extends ImageGray<T>, D extends ImageGray<D>, TF extends ImageGray<TF>>
		implements StereoDisparity<T,D>
{
	DisparityBlockMatchRowFormat<TF,D> alg;
	D disparity;
	TF adjustedLeft,adjustedRight;

	boolean normalizeInput=true;
	NormalizeParameters parameters = new NormalizeParameters();

	ImageType<T> inputType;

	public DisparityBlockMatchCorrelation(DisparityBlockMatchRowFormat<TF,D> alg, Class<T> inputType ) {
		this.alg = alg;
		this.inputType = ImageType.single(inputType);

		adjustedLeft = alg.getInputType().createImage(1,1);
		adjustedRight = alg.getInputType().createImage(1,1);
	}

	@Override
	public void process(T imageLeft, T imageRight) {
		if( disparity == null || disparity.width != imageLeft.width || disparity.height != imageLeft.height )  {
			// make sure the image borders are marked as invalid
			disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(),imageLeft.width,imageLeft.height);
			GImageMiscOps.fill(disparity, getMaxDisparity() - getMinDisparity() + 1);
			// TODO move this outside and run it every time. Need to fill border
			//      left border will be radius + min disparity
		}

		if( normalizeInput ) {
			// normalize to reduce numerical problems, e.g. overflow/underflow
			ImageNormalization.zeroMeanMaxOne(imageLeft, adjustedLeft, parameters);
			// Here I'm assuming the cameras have their gain/exposure synchronized so you want to use the same
			// parameters or else you might degrade your performance.
			ImageNormalization.apply(imageRight, parameters, adjustedRight);
		} else {
			GConvertImage.convert(imageLeft,adjustedLeft);
			GConvertImage.convert(imageRight,adjustedRight);
		}
		alg.process(adjustedLeft,adjustedRight,disparity);
	}

	@Override
	public D getDisparity() {
		return disparity;
	}

	@Override
	public int getBorderX() {
		return alg.getBorderX();
	}

	@Override
	public int getBorderY() {
		return alg.getBorderY();
	}

	@Override
	public int getMinDisparity() {
		return alg.getMinDisparity();
	}

	@Override
	public int getMaxDisparity() {
		return alg.getMaxDisparity();
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}

	@Override
	public Class<D> getDisparityType() {
		return alg.getDisparityType();
	}

	public DisparityBlockMatchRowFormat<TF,D> getAlg() {
		return alg;
	}

	public TF getAdjustedLeft() {
		return adjustedLeft;
	}

	public TF getAdjustedRight() {
		return adjustedRight;
	}

	public boolean isNormalizeInput() {
		return normalizeInput;
	}

	public void setNormalizeInput(boolean normalizeInput) {
		this.normalizeInput = normalizeInput;
	}
}
