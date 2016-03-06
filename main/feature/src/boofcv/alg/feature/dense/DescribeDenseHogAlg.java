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
import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Implementation of the Histogram of Oriented Gradients (HOG) [1] dense feature descriptor.  Several variants
 * are described in the paper.  The algorithm used here is the "R-HOG unsigned orientation" variant.  The descriptor
 * is computed from a regular grid of cells and an unsigned histogram is computed.  Unsigned as in the angle is
 * from 0 to 180 degrees instead of 0 to 360.
 * </p>
 *
 * This is a (hopefully) faithful implementation to the algorithm described in the paper.  The descriptors are
 * computed with the following steps.
 * <ol>
 * <li>Compute image gradient using [-1,0,1] kernel</li>
 * <li>Compute histogram for each cell in the image</li>
 * <li>Compute descriptor from blocks of cells</li>
 * <li>Normalize descriptor using SIFT style L2-Hys normalization</li>
 * </ol>
 *
 * <h3>Cell Histogram</h3>
 * The image is broken up into a regular grid of "cells".  Every cell is square with a width of N pixels, where N
 * is a user specified parameter.  Within each cell a histogram is computed.  The number of histogram bins B is
 * specified by the user.  The histogram is created by computing the Euclidean norm for each pixel's gradient
 * inside the cell and then using bilinear interpolation to put it into the two neighboring bins which best match
 * its angle.
 *
 * <h3>Descriptor Blocks</h3>
 * Square blocks, which are M cells wide, are used to compute each descriptor.  The histogram is copied into a
 * {@link TupleDesc_F64} in a row-major fashion and a weight is applied.  The weight is specified for each cell
 * based on its distance from the block's center.  Gaussian weight is used with a standard deviation of 0.5*block_width.
 * Note that blocks do overlap.  See paper for details.
 *
 * <h3>Descriptor Normalization</h3>
 * First L2-normalization is applied to the descriptor.  Then min(0.2,desc[i]) is applied to all elements in the
 * descriptor.  After which L2-normalization is applied again.
 *
 * <h3>Accessing Results</h3>
 * A list of descriptor and their locations is available. The location refers to the top-left most pixel in
 * the region the descriptor is computed from.  These lists are computed in a regular grid with row-major ordering.
 * A request can be made for all descriptors computed from inside a rectangular region.
 *
 * <h3>Multi-Band Images</h3>
 * The gradient is computed for each band individually.  The band with the largest magnitude at that specific
 * pixel is used as the gradient for the pixel.
 *
 * <p>[1] Dalal, Navneet, and Bill Triggs. "Histograms of oriented gradients for human detection." Computer
 * Vision and Pattern Recognition, 2005. CVPR 2005.</p>
 *
 * @author Peter Abeles
 */
public abstract class DescribeDenseHogAlg<Input extends ImageBase, Derivative extends ImageBase> {

	ImageGradient<Input, Derivative> gradient;

	// gradient of each pixel
	protected Derivative derivX,derivY;

	FastQueue<TupleDesc_F64> descriptions;

	// Location of each descriptor in the image, top-left corner (lower extents)
	FastQueue<Point2D_I32> locations = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	int orientationBins; // number of orientation bins computed in a block
	int widthCell; // number of pixels wide a cell is
	int widthBlock;  // number of cells wide a block is
	int stepBlock; // how many cells are skipped between a block

	// storage for histograms in each cell
	Cell cells[] = new Cell[0];
	// number of cell rows and columns in the image
	int cellRows,cellCols;

	// storage for pixel gradient
	protected float pixelDX,pixelDY;

	// weights applied to each
	double weights[];

	// type of input image
	ImageType<Input> imageType;

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param widthCell Number of pixel's wide a cell is.  8 recommended
	 * @param widthBlock Number of cells's wide a black is. 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public DescribeDenseHogAlg(int orientationBins , int widthCell , int widthBlock ,
							   int stepBlock ,
							   ImageType<Input> imageType )
	{
		if( stepBlock <= 0 )
			throw new IllegalArgumentException("stepBlock must be >= 1");

		this.imageType = imageType;
		gradient = FactoryDerivative.gradient(DerivativeType.THREE,imageType,null);
		ImageType<Derivative> derivType = gradient.getDerivativeType();

		derivX = derivType.createImage(1,1);
		derivY = derivType.createImage(1,1);

		this.orientationBins = orientationBins;
		this.widthCell = widthCell;
		this.widthBlock = widthBlock;
		this.stepBlock = stepBlock;

		descriptions = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DescribeDenseHogAlg.this.orientationBins*
						DescribeDenseHogAlg.this.widthBlock *DescribeDenseHogAlg.this.widthBlock);
			}
		};

		computeCellWeights();
	}

	/**
	 * Computes the value of weights inside of a block
	 */
	protected void computeCellWeights() {
		Kernel2D_F64 kernel = FactoryKernelGaussian.gaussian2D_F64(0.5*widthBlock,widthBlock/2,widthBlock%2==1,false);
		KernelMath.normalizeMaxOne(kernel);
		weights = kernel.data;
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

	/**
	 * Computes the descriptor across the input image
	 */
	public void process() {
		locations.reset();
		descriptions.reset();

		// see if the cell array needs to grow for this image.  Recycle data when growing
		growCellArray(derivX.width, derivX.height);

		computeCellHistograms();

		int cellRowMax = (cellRows - (widthBlock-1));
		int cellColMax = (cellCols - (widthBlock-1));

		for (int i = 0; i < cellRowMax; i += stepBlock) {
			for (int j = 0; j < cellColMax; j += stepBlock) {
				computeDescriptor(i,j);
			}
		}

	}

	/**
	 * Determines if the cell array needs to grow.  If it does a new array is declared.  Old data is recycled when
	 * possible
	 */
	void growCellArray(int imageWidth, int imageHeight) {
		cellCols = imageWidth/ widthCell;
		cellRows = imageHeight/ widthCell;

		if( cellRows*cellCols > cells.length ) {
			Cell[] a = new Cell[cellCols*cellRows];

			System.arraycopy(cells,0,a,0,cells.length);
			for (int i = cells.length; i < a.length; i++) {
				a[i] = new Cell();
				a[i].histogram = new float[ orientationBins ];
			}
			cells = a;
		}
	}

	/**
	 * Convenience function which returns a list of all the descriptors computed inside the specified region in the image
	 *
	 * @param pixelX0 Pixel coordinate X-axis lower extent
	 * @param pixelY0 Pixel coordinate Y-axis lower extent
	 * @param pixelX1 Pixel coordinate X-axis upper extent
	 * @param pixelY1 Pixel coordinate Y-axis upper extent
	 * @param output List of descriptions
	 */
	public void getDescriptorsInRegion(int pixelX0 , int pixelY0 , int pixelX1 , int pixelY1 ,
									   List<TupleDesc_F64> output ) {
		int gridX0 = (int)Math.ceil(pixelX0/(double)widthCell);
		int gridY0 = (int)Math.ceil(pixelY0/(double)widthCell);

		int gridX1 = pixelX1/widthCell - widthBlock;
		int gridY1 = pixelY1/widthCell - widthBlock;

		for (int y = gridY0; y <= gridY1; y++) {
			int index = y*cellCols + gridX0;
			for (int x = gridX0; x <= gridX1; x++ ) {
				output.add( descriptions.get(index++) );
			}
		}
	}

	/**
	 * Compute the descriptor from the specified cells.  (row,col) to (row+w,col+w)
	 * @param row Lower extent of cell rows
	 * @param col Lower extent of cell columns
	 */
	void computeDescriptor(int row, int col) {
		// set location to top-left pixel
		locations.grow().set(col*widthCell,row*widthCell);

		TupleDesc_F64 d = descriptions.grow();

		int indexDesc = 0;
		for (int i = 0; i < widthBlock; i++) {
			for (int j = 0; j < widthBlock; j++) {
				Cell c = cells[(row+i)*cellCols + (col+j)];

				// copy the histogram into the descriptor while applying cell specific weighting
				double w = weights[i*widthBlock+j];

				for (int k = 0; k < c.histogram.length; k++) {
					d.value[indexDesc++] = c.histogram[k]*w;
				}
			}
		}

		// Apply SIFT style L2-Hys normalization
		DescribeSiftCommon.normalizeDescriptor(d,0.2);
	}

	/**
	 * Compute histograms for all the cells inside the image using precomputed derivative.

	 */
	void computeCellHistograms() {

		int width = cellCols*widthCell;
		int height = cellRows*widthCell;

		float angleBinSize = GrlConstants.F_PI/orientationBins;

		int indexCell = 0;
		for (int i = 0; i < height; i += widthCell) {
			for (int j = 0; j < width; j += widthCell, indexCell++ ) {
				Cell c = cells[indexCell];
				c.reset();

				for (int k = 0; k < widthCell; k++) {
					int indexPixel = (i+k)*derivX.width+j;

					for (int l = 0; l < widthCell; l++, indexPixel++ ) {
						computeDerivative(indexPixel);

						// angle from 0 to pi radians
						float angle = UtilAngle.atanSafe(pixelDY,pixelDX) + GrlConstants.F_PId2;

						// gradient magnitude
						float magnitude = (float)Math.sqrt(pixelDX*pixelDX + pixelDY*pixelDY);

						// Add the weighted gradient using bilinear interpolation
						float findex0 = angle/angleBinSize;
						int index0 = (int)findex0;
						float weight1 = findex0-index0;
						index0 %= orientationBins;
						int index1 = (index0+1)%orientationBins;

						c.histogram[index0] += magnitude*(1.0f-weight1);
						c.histogram[index1] += magnitude*weight1;
					}
				}

			}
		}
	}

	/**
	 * Computes and stores the gradient at the specified pixel.  pixelIndex = y*columns + x
	 */
	public abstract void computeDerivative( int pixelIndex );

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

	public Derivative _getDerivX() {
		return derivX;
	}

	public Derivative _getDerivY() {
		return derivY;
	}

	public float _getPixelDX() {
		return pixelDX;
	}

	public float _getPixelDY() {
		return pixelDY;
	}

	/**
	 * Returns the number of pixel's wide the square region is that a descriptor was computed from
	 * @return number of pixels wide
	 */
	public int getRegionWidthPixel() {
		return widthCell*widthBlock;
	}

	public void setWidthCell(int widthCell) {
		this.widthCell = widthCell;
	}

	public int getWidthCell() {
		return widthCell;
	}

	public int getWidthBlock() {
		return widthBlock;
	}

	public int getStepBlock() {
		return stepBlock;
	}

	public int getOrientationBins() {
		return orientationBins;
	}

	public int getCellRows() {
		return cellRows;
	}

	public int getCellCols() {
		return cellCols;
	}

	public Cell getCell( int row , int col ) {
		return cells[row*cellCols + col];
	}

	public void setStepBlock(int stepBlock) {
		this.stepBlock = stepBlock;
	}

	public ImageType<Input> getImageType() {
		return imageType;
	}

	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(orientationBins*widthBlock*widthBlock);
	}

	public static class Cell
	{
		public float histogram[];

		public void reset() {
			Arrays.fill(histogram,0);
		}
	}
}
