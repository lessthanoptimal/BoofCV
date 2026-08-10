/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.disparity;

import boofcv.alg.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.ImageNormalization;
import boofcv.alg.misc.NormalizeParameters;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;

/// Wrapper around [StereoDisparity] that will (optionally) apply a pre-process normalization which improves
/// the numerical stability. See [Type] for a description of different types of normalization it will do.
public class DisparityBlockMatchPreFilter<T extends ImageGray<T>, D extends ImageGray<D>, TF extends ImageGray<TF>>
		extends WrapBaseBlockMatch<T, TF, D> {
	@Getter TF adjustedLeft, adjustedRight;

	@Getter @Setter Type type = Type.NONE;
	@Getter NormalizeParameters parameters = new NormalizeParameters();

	@Getter ImageType<T> inputType;

	public DisparityBlockMatchPreFilter( DisparityBlockMatchRowFormat<TF, D> alg, Class<T> inputType ) {
		super(alg);
		this.inputType = ImageType.single(inputType);

		adjustedLeft = alg.getInputType().createImage(1, 1);
		adjustedRight = alg.getInputType().createImage(1, 1);
	}

	@Override
	public void _process( T imageLeft, T imageRight ) {

		switch (type) {
			case CORRELATION -> {
				// normalize to reduce numerical problems, e.g. overflow/underflow
				ImageNormalization.zeroMeanMaxOne(imageLeft, adjustedLeft, parameters);
				// Here I'm assuming the cameras have their gain/exposure synchronized so you want to use the same
				// parameters or else you might degrade your performance.
				ImageNormalization.apply(imageRight, parameters, adjustedRight);
			}

			default -> {
				GConvertImage.convert(imageLeft, adjustedLeft);
				GConvertImage.convert(imageRight, adjustedRight);
			}
		}

		alg.process(adjustedLeft, adjustedRight, disparity, score);
	}

	/// Type of prefilter normalization
	public enum Type {
		/// Correlation specific. left image is zero-mean and max of one, right has the same norm applied to it
		CORRELATION,
		/// No normalization is done, but the input images are converted into float
		NONE,
	}
}
