/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.ImageNormalization;
import boofcv.alg.misc.NormalizeParameters;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link StereoDisparity} that will (optionally) convert all inputs to float and normalize the input to have
 * zero mean and an absolute value of at most 1.
 *
 * @author Peter Abeles
 */
public class DisparityBlockMatchCorrelation<T extends ImageGray<T>, D extends ImageGray<D>, TF extends ImageGray<TF>>
		extends WrapBaseBlockMatch<T,TF,D>
{
	TF adjustedLeft,adjustedRight;

	boolean normalizeInput=true;
	NormalizeParameters parameters = new NormalizeParameters();

	ImageType<T> inputType;

	public DisparityBlockMatchCorrelation(DisparityBlockMatchRowFormat<TF,D> alg, Class<T> inputType ) {
		super(alg);
		this.inputType = ImageType.single(inputType);

		adjustedLeft = alg.getInputType().createImage(1,1);
		adjustedRight = alg.getInputType().createImage(1,1);
	}

	@Override
	public void _process(T imageLeft, T imageRight) {
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
	public ImageType<T> getInputType() {
		return inputType;
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
