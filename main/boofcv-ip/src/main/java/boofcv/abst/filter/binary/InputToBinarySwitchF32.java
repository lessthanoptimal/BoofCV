/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.*;

/**
 * Wrapper around {@link ThresholdBlockOtsu}
 *
 * @author Peter Abeles
 */
public class InputToBinarySwitchF32<T extends ImageGray<T>> implements InputToBinary<T> {

	ImageType<T> inputType;

	InputToBinary<GrayF32> alg;
	GrayF32 input;

	/**
	 * @see ThresholdBlockOtsu
	 */
	public InputToBinarySwitchF32(InputToBinary<GrayF32> alg,
								  Class<T> inputType) {

		this.alg = alg;
		this.inputType = ImageType.single(inputType);

		if( this.inputType.getDataType() != ImageDataType.F32 ) {
			input = new GrayF32(1,1);
		}
	}

	@Override
	public void process(T input, GrayU8 output) {
		if( this.input == null )
			alg.process((GrayF32)input,output);
		else {
			this.input.reshape(input.width,input.height);
			GConvertImage.convert(input,this.input);
			alg.process(this.input,output);
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}
}
