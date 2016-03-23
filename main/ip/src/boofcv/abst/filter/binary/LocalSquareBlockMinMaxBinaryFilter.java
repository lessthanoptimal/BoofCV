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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ThresholdSquareBlockMinMax;
import boofcv.alg.filter.binary.impl.ThresholdSquareBlockMinMax_F32;
import boofcv.alg.filter.binary.impl.ThresholdSquareBlockMinMax_U8;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link ThresholdSquareBlockMinMax}.
 *
 * @author Peter Abeles
 */
public class LocalSquareBlockMinMaxBinaryFilter<T extends ImageGray>
		implements InputToBinary<T>
{
	ThresholdSquareBlockMinMax alg;
	ImageType<T> imageType;

	public LocalSquareBlockMinMaxBinaryFilter(double minimumSpread, int requestedBlockWidth, double scale , boolean down, Class<T> imageType ) {

		if( imageType == GrayF32.class )
			this.alg = new ThresholdSquareBlockMinMax_F32((float)minimumSpread,requestedBlockWidth,(float)scale,down);
		else if( imageType == GrayU8.class )
			this.alg = new ThresholdSquareBlockMinMax_U8(minimumSpread,requestedBlockWidth,scale,down);
		else
			throw new IllegalArgumentException("Unsupported image type");

		this.imageType = ImageType.single(imageType);
	}

	@Override
	public void process(T input, GrayU8 output) {
		alg.process(input,output);
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
		return imageType;
	}

	@Override
	public ImageType<GrayU8> getOutputType() {
		return ImageType.single(GrayU8.class);
	}
}
