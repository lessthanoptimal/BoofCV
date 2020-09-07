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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Base class for computing global thresholds
 *
 * @see GThresholdImageOps#computeOtsu(ImageGray, double, double)
 *
 * @author Peter Abeles
 */
public abstract class GlobalBinaryFilter<T extends ImageGray<T>> implements InputToBinary<T> {

	ImageType<T> inputType;

	// scales the threshold up or down. meaning of scale depends on direction of threshold. See  code
	double scale;
	// direction of thresholding
	boolean down;
	// min and max possible pixel values
	double minValue;
	double maxValue;

	/**
	 * @see GThresholdImageOps#computeOtsu
	 */
	protected GlobalBinaryFilter(double minValue, double maxValue, double scale,
							  boolean down, ImageType<T> inputType) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.down = down;
		this.scale = scale;
		this.inputType = inputType;
	}

	@Override
	public void process(T input, GrayU8 output) {
		double scale = down ? this.scale : (this.scale<=0.0 ? 0.0 : 1.0/ this.scale);
		double threshold = computeThreshold(input) * scale;
		GThresholdImageOps.threshold(input,output,threshold,down);
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}

	abstract double computeThreshold( T input );

	/**
	 * Computes a threshold using Otsu's equation.
	 *
	 * @see GThresholdImageOps#computeOtsu(ImageGray, double, double)
	 *
	 * @author Peter Abeles
	 */
	public static class Otsu<T extends ImageGray<T>> extends GlobalBinaryFilter<T>{

		public Otsu(double minValue, double maxValue, double scale, boolean down, ImageType<T> inputType) {
			super(minValue, maxValue, scale, down, inputType);
		}

		@Override
		double computeThreshold(T input) {
			return GThresholdImageOps.computeOtsu(input,minValue,maxValue);
		}
	}

	/**
	 * Computes a threshold using Li's equation.
	 *
	 * @see GThresholdImageOps#computeLi(ImageGray, double, double)
	 *
	 * @author Peter Abeles
	 */
	public static class Li<T extends ImageGray<T>> extends GlobalBinaryFilter<T>{
		public Li(double minValue, double maxValue, double scale, boolean down, ImageType<T> inputType) {
			super(minValue, maxValue, scale, down, inputType);
		}

		@Override
		double computeThreshold(T input) {
			return GThresholdImageOps.computeLi(input,minValue,maxValue);
		}
	}

	/**
	 * Computes a threshold using Huang's equation.
	 *
	 * @see GThresholdImageOps#computeHuang(ImageGray, double, double)
	 *
	 * @author Peter Abeles
	 */
	public static class Huang<T extends ImageGray<T>> extends GlobalBinaryFilter<T>{
		public Huang(double minValue, double maxValue, double scale, boolean down, ImageType<T> inputType) {
			super(minValue, maxValue, scale, down, inputType);
		}

		@Override
		double computeThreshold(T input) {
			return GThresholdImageOps.computeHuang(input,minValue,maxValue);
		}
	}

	/**
	 * Computes a threshold based on entropy to create a binary image
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeEntropy(ImageGray, double, double)
	 *
	 * @author Peter Abeles
	 */
	public static class Entropy<T extends ImageGray<T>> extends GlobalBinaryFilter<T>{
		public Entropy(double minValue, double maxValue, double scale, boolean down, ImageType<T> inputType) {
			super(minValue, maxValue, scale, down, inputType);
		}

		@Override
		double computeThreshold(T input) {
			return GThresholdImageOps.computeEntropy(input,minValue,maxValue);
		}
	}
}
