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
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link ThresholdBlockOtsu}
 *
 * @author Peter Abeles
 */
public class LocalBlockOtsuBinaryFilter<T extends ImageGray<T>> implements InputToBinary<T> {

	ImageType<T> inputType;

	ThresholdBlockOtsu alg;
	GrayU8 input;

	/**
	 * @see ThresholdBlockOtsu
	 */
	public LocalBlockOtsuBinaryFilter(int requestedBlockWidth, double scale, boolean down,
									  ImageType<T> inputType) {

		this.inputType = inputType;

		if( inputType.getDataType() != ImageDataType.U8 ) {
			input = new GrayU8(1,1);
		}

		alg = new ThresholdBlockOtsu(requestedBlockWidth,scale,down);
	}

	@Override
	public void process(T input, GrayU8 output) {
		if( this.input == null )
			alg.process((GrayU8)input,output);
		else {
			this.input.reshape(input.width,input.height);
			GConvertImage.convert(input,this.input);
			alg.process(this.input,output);
		}
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<GrayU8> getOutputType() {
		return ImageType.single(GrayU8.class);
	}
}
