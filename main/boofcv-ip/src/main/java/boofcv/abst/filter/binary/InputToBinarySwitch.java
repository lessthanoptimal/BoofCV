/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ThresholdBlockOtsu;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * {@link InputToBinary} which will convert the input image into the specified type prior to processing.
 *
 * @author Peter Abeles
 */
public class InputToBinarySwitch<T extends ImageGray<T>> implements InputToBinary<T> {

	ImageType<T> inputType;

	InputToBinary alg;
	ImageGray work;

	/**
	 * @see ThresholdBlockOtsu
	 */
	public InputToBinarySwitch(InputToBinary alg,
							   Class<T> inputType) {

		this.alg = alg;
		this.inputType = ImageType.single(inputType);

		if( !alg.getInputType().isSameType(this.inputType)) {
			work = (ImageGray)alg.getInputType().createImage(1,1);
		}
	}

	@Override
	public void process(T input, GrayU8 output) {
		if( this.work == null )
			alg.process(input,output);
		else {
			this.work.reshape(input.width,input.height);
			GConvertImage.convert(input,this.work);
			alg.process(this.work,output);
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}
}
