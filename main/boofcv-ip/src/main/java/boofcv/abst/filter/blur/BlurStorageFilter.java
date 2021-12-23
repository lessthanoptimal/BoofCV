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

package boofcv.abst.filter.blur;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * Simplified interface for using a blur filter that requires storage. Reflections are used to look up a function inside
 * of {@link boofcv.alg.filter.blur.BlurImageOps} which is then invoked later on.
 *
 * @author Peter Abeles
 */
public class BlurStorageFilter<T extends ImageBase<T>> implements BlurFilter<T> {

	// Wrapper around performed operation
	private BlurOperation operation;

	// the Gaussian's standard deviation
	private double sigmaX, sigmaY;
	// size of the blur region along each axis
	private int radiusX, radiusY;
	// stores intermediate results
	private T storage;

	// type of image it processes
	ImageType<T> inputType;
	GrowArray<?> growArray;

	/** Specified how the border is handled for mean images. If null then it's normalized */
	@Getter @Setter @Nullable ImageBorder<T> border = null;

	public BlurStorageFilter( String functionName, ImageType<T> inputType, int radius ) {
		this(functionName, inputType, -1, radius, -1, radius);
	}

	public BlurStorageFilter( String functionName, ImageType<T> inputType, int radiusX, int radiusY ) {
		this(functionName, inputType, -1, radiusX, -1, radiusY);
	}

	public BlurStorageFilter( String functionName, ImageType<T> inputType,
							  double sigmaX, int radiusX, double sigmaY, int radiusY ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.sigmaX = sigmaX;
		this.sigmaY = sigmaY;
		this.inputType = inputType;

		switch (functionName) {
			case "mean" -> operation = new MeanOperation();
			case "meanB" -> operation = new MeanBorderOperation();
			case "gaussian" -> operation = new GaussianOperation();
			case "median" -> {
				if (radiusX != radiusY)
					throw new IllegalArgumentException("Median currently only supports equal radius");
				operation = new MedianOperator();
			}
			default -> throw new IllegalArgumentException("Unknown function " + functionName);
		}

		growArray = GeneralizedImageOps.createGrowArray(inputType);
		createStorage();
	}

	private void createStorage() {
		if (inputType.getFamily() == ImageType.Family.PLANAR) {
			storage = (T)GeneralizedImageOps.createSingleBand(inputType.getImageClass(), 1, 1);
		} else {
			storage = inputType.createImage(1, 1);
		}
	}

	/**
	 * Radius of the square region. The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	@Override
	public int getRadius() {
		return radiusX;
	}

	@Override
	public void setRadius( int radius ) {
		this.radiusX = radius;
		this.radiusY = radius;
	}

	@Override
	public void process( T input, T output ) {
		if (storage != null)
			storage.reshape(output.width, output.height);
		operation.process(input, output);
	}

	@Override
	public int getBorderX() { // TODO meanBorder filter can now just process the inner image. This needs to be updated
		return 0;
	}

	@Override
	public int getBorderY() {
		return 0;
	}

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<T> getOutputType() {
		return inputType;
	}

	private interface BlurOperation {
		void process( ImageBase input, ImageBase output );
	}

	private class MeanOperation implements BlurOperation {
		@Override
		public void process( ImageBase input, ImageBase output ) {
			if (border != null)
				throw new IllegalArgumentException(
						"Border has been set but will never be used. Must be a bug. Use meanB() instead");
			GBlurImageOps.mean(input, output, radiusX, radiusY, storage, growArray);
		}
	}

	private class MeanBorderOperation implements BlurOperation {
		@Override
		public void process( ImageBase input, ImageBase output ) {
			GBlurImageOps.meanB(input, output, radiusX, radiusY, (ImageBorder)border, storage, growArray);
		}
	}

	private class GaussianOperation implements BlurOperation {
		@Override
		public void process( ImageBase input, ImageBase output ) {
			if (border != null)
				throw new IllegalArgumentException("Border has been set but will never be used. Must be a bug.");
			GBlurImageOps.gaussian(input, output, sigmaX, radiusX, sigmaY, radiusY, storage);
		}
	}

	private class MedianOperator implements BlurOperation {
		@Override
		public void process( ImageBase input, ImageBase output ) {
			if (border != null)
				throw new IllegalArgumentException("Border has been set but will never be used. Must be a bug.");
			GBlurImageOps.median(input, output, radiusX, radiusY, growArray);
		}
	}
}
