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

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Adaptive/local threshold using a square region
 *
 * @see boofcv.alg.filter.binary.GThresholdImageOps#localMean(ImageGray, GrayU8, ConfigLength, double, boolean, ImageGray, ImageGray)
 *
 * @author Peter Abeles
 */
public class LocalMeanBinaryFilter<T extends ImageGray<T>> implements InputToBinary<T> {

	ImageType<T> inputType;

	T work1;
	ImageGray work2;

	ConfigLength regionWidth;
	double scale;
	boolean down;

	/**
	 * @see GThresholdImageOps#localMean
	 */
	public LocalMeanBinaryFilter(ConfigLength width, double scale, boolean down,
								 ImageType<T> inputType) {
		this.regionWidth = width;
		this.scale = scale;
		this.down = down;
		this.inputType = inputType;
		work1 = inputType.createImage(1,1);
		work2 = inputType.createImage(1,1);
	}

	@Override
	public void process(T input, GrayU8 output) {
		work1.reshape(input.width,input.height);
		work2.reshape(input.width,input.height);
		GThresholdImageOps.localMean((T)input, output, regionWidth, scale, down, (T)work1, (T)work2);
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}

}
