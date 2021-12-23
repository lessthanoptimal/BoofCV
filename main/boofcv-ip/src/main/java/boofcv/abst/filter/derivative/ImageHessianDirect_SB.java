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

package boofcv.abst.filter.derivative;

import boofcv.BoofDefaults;
import boofcv.alg.filter.derivative.HessianSobel;
import boofcv.alg.filter.derivative.HessianThree;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Generic implementation which uses reflections to call hessian functions
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ImageHessianDirect_SB<Input extends ImageGray<Input>, Output extends ImageGray<Output>>
		implements ImageHessianDirect<Input, Output> {
	// How the image border should be handled
	BorderType borderType = BoofDefaults.DERIV_BORDER_TYPE;
	ImageBorder<Input> border;
	Class<Input> inputType;
	Class<Output> derivType;

	protected ImageHessianDirect_SB( Class<Input> inputType, Class<Output> derivType ) {
		this.inputType = inputType;
		this.derivType = derivType;
		setBorderType(borderType);
	}

	@Override
	public void setBorderType( BorderType type ) {
		this.borderType = type;
		border = FactoryImageBorder.single(borderType, inputType);
	}

	@Override
	public BorderType getBorderType() {
		return borderType;
	}

	@Override
	public int getBorder() {
		if (borderType != BorderType.SKIP)
			return 0;
		else
			return 1;
	}

	//	@Override
	public ImageType<Input> getInputType() {
		return ImageType.single(inputType);
	}

	@Override
	public ImageType<Output> getDerivativeType() {
		return ImageType.single(derivType);
	}

	public static class Sobel<T extends ImageGray<T>, D extends ImageGray<D>> extends ImageHessianDirect_SB<T, D> {
		public Sobel( Class<T> inputType, Class<D> derivType ) {super(inputType, derivType);}

		@Override
		public void process( T inputImage, D derivXX, D derivYY, D derivXY ) {
			HessianSobel.process(inputImage, derivXX, derivYY, derivXY, border);
		}
	}

	public static class Three<T extends ImageGray<T>, D extends ImageGray<D>> extends ImageHessianDirect_SB<T, D> {
		public Three( Class<T> inputType, Class<D> derivType ) {super(inputType, derivType);}

		@Override
		public void process( T inputImage, D derivXX, D derivYY, D derivXY ) {
			HessianThree.process(inputImage, derivXX, derivYY, derivXY, border);
		}
	}
}
