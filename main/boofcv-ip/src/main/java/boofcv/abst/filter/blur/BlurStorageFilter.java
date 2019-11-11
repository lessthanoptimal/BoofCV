/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.concurrency.WorkArrays;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Simplified interface for using a blur filter that requires storage.  Reflections are used to look up a function inside
 * of {@link boofcv.alg.filter.blur.BlurImageOps} which is then invoked later on.
 *
 * @author Peter Abeles
 */
public class BlurStorageFilter<T extends ImageBase<T>> implements BlurFilter<T> {

	// Wrapper around performed operation
	private BlurOperation operation;

	// the Gaussian's standard deviation
	private double sigma;
	// size of the blur region along each axis
	private int radiusX,radiusY;
	// stores intermediate results
	private T storage;

	// type of image it processes
	ImageType<T> inputType;
	WorkArrays workArray;

	public BlurStorageFilter( String functionName , ImageType<T> inputType, int radius) {
		this(functionName,inputType,-1,radius,radius);
	}

	public BlurStorageFilter( String functionName , ImageType<T> inputType, int radiusX, int radiusY) {
		this(functionName,inputType,-1,radiusX,radiusY);
	}

	public BlurStorageFilter( String functionName , ImageType<T> inputType, double sigma , int radiusX, int radiusY) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.sigma = sigma;
		this.inputType = inputType;

		if( functionName.equals("mean")) {
			operation = new MeanOperation();
			createStorage();
		} else if( functionName.equals("gaussian")) {
			if( radiusX != radiusY )
				throw new IllegalArgumentException("Gaussian currently only supports equal radius");
			operation = new GaussianOperation();
			createStorage();
		} else if( functionName.equals("median")) {
			if( radiusX != radiusY )
				throw new IllegalArgumentException("Median currently only supports equal radius");
			operation = new MedianOperator();
		} else {
			throw new IllegalArgumentException("Unknown function "+functionName);
		}

		workArray = GeneralizedImageOps.createWorkArray( inputType );
	}

	private void createStorage() {
		if( inputType.getFamily() == ImageType.Family.PLANAR ) {
			storage = (T)GeneralizedImageOps.createSingleBand(inputType.getImageClass(),1,1);
		} else {
			storage = inputType.createImage(1,1);
		}
	}

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	@Override
	public int getRadius() {
		return radiusX;
	}

	@Override
	public void setRadius(int radius) {
		this.radiusX = radius;
		this.radiusY = radius;
	}

	@Override
	public void process(T input, T output) {
		if( storage != null )
			storage.reshape(output.width, output.height);
		operation.process(input,output);
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
	public ImageType<T> getOutputType() {
		return inputType;
	}

	private interface BlurOperation {
		public void process(ImageBase input , ImageBase output );
	}

	private class MeanOperation implements BlurOperation {
		@Override
		public void process(ImageBase input, ImageBase output) {
			GBlurImageOps.mean(input,output, radiusX,radiusY,storage,workArray);
		}
	}

	private class GaussianOperation implements BlurOperation {
		@Override
		public void process(ImageBase input, ImageBase output) {
			GBlurImageOps.gaussian(input,output,sigma, radiusX,storage);
		}
	}

	private class MedianOperator implements BlurOperation {
		@Override
		public void process(ImageBase input, ImageBase output) {
			GBlurImageOps.median(input,output, radiusX,workArray);
		}
	}
}
