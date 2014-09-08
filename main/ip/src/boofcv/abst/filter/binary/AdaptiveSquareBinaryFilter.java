/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * Adaptive/local threshold using a square region
 *
 * @see boofcv.alg.filter.binary.GThresholdImageOps#adaptiveSquare(boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageUInt8, int, double, boolean, boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageSingleBand)
 *
 * @author Peter Abeles
 */
public class AdaptiveSquareBinaryFilter<T extends ImageSingleBand> implements InputToBinary<T> {

	ImageType<T> inputType;

	T work1;
	ImageSingleBand work2;

	int radius;
	double bias;
	boolean down;

	public AdaptiveSquareBinaryFilter(int radius, double bias, boolean down,
									  ImageType<T> inputType) {
		this.radius = radius;
		this.bias = bias;
		this.down = down;
		this.inputType = inputType;
		work1 = inputType.createImage(1,1);
		work2 = inputType.createImage(1,1);
	}

	@Override
	public void process(T input, ImageUInt8 output) {
		work1.reshape(input.width,input.height);
		work2.reshape(input.width,input.height);
		GThresholdImageOps.adaptiveSquare(input,output,radius,bias,down,work1,work2);
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
	public ImageType<ImageUInt8> getOutputType() {
		return ImageType.single(ImageUInt8.class);
	}
}
