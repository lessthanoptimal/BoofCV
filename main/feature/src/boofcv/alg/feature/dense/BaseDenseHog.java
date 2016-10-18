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

package boofcv.alg.feature.dense;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.DerivativeReduceType;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * Base calss for dense HOG implementations.
 *
 * @author Peter Abeles
 */
public abstract class BaseDenseHog<Input extends ImageBase> {

	ImageGradient<Input, GrayF32> gradient;

	// gradient of each pixel
	protected GrayF32 derivX = new GrayF32(1,1);
	protected GrayF32 derivY = new GrayF32(1,1);

	// Storage for descriptors
	FastQueue<TupleDesc_F64> descriptions;

	// Location of each descriptor in the image, top-left corner (lower extents)
	FastQueue<Point2D_I32> locations = new FastQueue<>(Point2D_I32.class, true);

	int orientationBins; // number of orientation bins computed in a block
	int pixelsPerCell; // number of pixels wide a cell is
	int cellsPerBlockX;  // number of cells wide a block is
	int cellsPerBlockY;  // number of cells wide a block is
	int stepBlock; // how many cells are skipped between a block

	// type of input image
	ImageType<Input> imageType;

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param pixelsPerCell Number of pixel's wide a cell is.  8 recommended
	 * @param cellsPerBlockX Number of cells's wide a block is. x-axis 3 recommended
	 * @param cellsPerBlockY Number of cells's wide a block is. x-axis 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public BaseDenseHog(int orientationBins , int pixelsPerCell ,
						int cellsPerBlockX , int cellsPerBlockY,
						int stepBlock ,
						ImageType<Input> imageType )
	{
		if( stepBlock <= 0 )
			throw new IllegalArgumentException("stepBlock must be >= 1");

		this.imageType = imageType;

		gradient = createGradient(imageType);

		this.orientationBins = orientationBins;
		this.pixelsPerCell = pixelsPerCell;
		this.cellsPerBlockX = cellsPerBlockX;
		this.cellsPerBlockY = cellsPerBlockY;
		this.stepBlock = stepBlock;

		final int descriptorLength = orientationBins*cellsPerBlockX*cellsPerBlockY;

		descriptions = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(descriptorLength);
			}
		};
	}

	/**
	 * Given different types input images it creates the correct algorithm for computing the image gradient.  The
	 * actualy calulcation is always done using {@link DerivativeType#THREE}
	 */
	static <Input extends ImageBase>
	ImageGradient<Input,GrayF32> createGradient( ImageType<Input> imageType ) {
		ImageGradient<Input,GrayF32> gradient;
		ImageType<GrayF32> typeF32 = ImageType.single(GrayF32.class);

		if( imageType.getDataType() != ImageDataType.F32 )
			throw new IllegalArgumentException("Input image type must be F32");

		if( imageType.getFamily() == ImageType.Family.GRAY) {
			gradient = FactoryDerivative.gradient(DerivativeType.THREE,imageType, typeF32);
		} else if( imageType.getFamily() == ImageType.Family.PLANAR ) {
			ImageType<Planar<GrayF32>> typePF32 = ImageType.pl(imageType.getNumBands(),GrayF32.class);
			ImageGradient<Planar<GrayF32>,Planar<GrayF32>> gradientMB =
					FactoryDerivative.gradient(DerivativeType.THREE,typePF32, typePF32);
			gradient = (ImageGradient)FactoryDerivative.gradientReduce(gradientMB, DerivativeReduceType.MAX_F, GrayF32.class);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType);
		}

		return gradient;
	}

	/**
	 * Specifies input image.  Gradient is computed immediately
	 * @param input input image
	 */
	public void setInput( Input input ) {
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);

		// pixel gradient
		gradient.process(input,derivX,derivY);
	}

	public abstract void process();

	/**
	 * List of locations for each descriptor.
	 */
	public FastQueue<Point2D_I32> getLocations() {
		return locations;
	}

	/**
	 * List of descriptors
	 */
	public FastQueue<TupleDesc_F64> getDescriptions() {
		return descriptions;
	}

	public GrayF32 _getDerivX() {
		return derivX;
	}

	public GrayF32 _getDerivY() {
		return derivY;
	}

	/**
	 * Returns the number of pixel's wide the square region is that a descriptor was computed from
	 * @return number of pixels wide
	 */
	public int getRegionWidthPixelX() {
		return pixelsPerCell * cellsPerBlockX;
	}

	public int getRegionWidthPixelY() {
		return pixelsPerCell * cellsPerBlockY;
	}

	public int getPixelsPerCell() {
		return pixelsPerCell;
	}

	public int getCellsPerBlockX() {
		return cellsPerBlockX;
	}

	public int getCellsPerBlockY() {
		return cellsPerBlockY;
	}

	public int getStepBlock() {
		return stepBlock;
	}

	public int getOrientationBins() {
		return orientationBins;
	}

	public ImageType<Input> getImageType() {
		return imageType;
	}

	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(orientationBins* cellsPerBlockX * cellsPerBlockY);
	}
}
