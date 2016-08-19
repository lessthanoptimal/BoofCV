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
import boofcv.alg.filter.derivative.DerivativeReduceType;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.Arrays;

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
 * <li>Compute magnitude and orientation for each pixel</li>
 * <li>For each pixel, spread out it's magnitude using interpolation.</li>
 * <li>Normalize descriptor using SIFT style L2-Hys normalization</li>
 * </ol>
 *
 * <h3>Cells and Blocks</h3>
 * The image is broken up into a regular grid of "cells".  Every cell is square with a width of N pixels, where N
 * is a user specified parameter.  A block is a region composed of cells and is M by M cells in size. The size of
 * the descriptor will be M by M by O, where O is the number of orientation histogram bins.
 *
 * <h3>Orientation Histogram</h3>
 * A histogram for each cell is computed.  Ignoring interpolation, it would be computed by finding the magnitude
 * and unsigned orientation of each pixel in the cell.  Magnitude is defined as the Euclidean norm of the gradient
 * and orientation is found to be 0 to PI radians.  The bin for the orientation is found and the magnitude added to it.
 * However, because of interpolation, each pixel contributes to multiple cells and orientation bins.
 *
 * <h3>Interpolation and Weighting</h3>
 * Per-pixel interpolation and weighting is applied when assigning a value to each orientation bin and
 * cell.  Linear interpolation is used for orientation.  Bilinear interpolation for assigning values to each cell
 * using the cell's center.  Gaussian weighting is applied to each pixel with the center at each block.  Each
 * pixel can contribute up to 4 different histogram bins, and the all the pixels in a cell can contribute to
 * the histogram of 9 cells.
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
public class DescribeDenseHogAlg<Input extends ImageBase> {

	ImageGradient<Input, GrayF32> gradient;

	// gradient of each pixel
	protected GrayF32 derivX = new GrayF32(1,1);
	protected GrayF32 derivY = new GrayF32(1,1);
	// orientation and magnitude of each pixel
	protected GrayF32 orientation = new GrayF32(1,1);
	protected GrayF64 magnitude = new GrayF64(1,1); // stored as F64 instead of F32 for speed

	// Storage for descriptors
	FastQueue<TupleDesc_F64> descriptions;

	// Location of each descriptor in the image, top-left corner (lower extents)
	FastQueue<Point2D_I32> locations = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	int orientationBins; // number of orientation bins computed in a block
	int pixelsPerCell; // number of pixels wide a cell is
	int cellsPerBlock;  // number of cells wide a block is
	int stepBlock; // how many cells are skipped between a block

	// the active histogram being worked on
	double histogram[];

	// spatial weights applied to each in a block
	// stored in a row major order
	double weights[];

	// type of input image
	ImageType<Input> imageType;

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param pixelsPerCell Number of pixel's wide a cell is.  8 recommended
	 * @param cellsPerBlock Number of cells's wide a block is. 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public DescribeDenseHogAlg(int orientationBins , int pixelsPerCell , int cellsPerBlock ,
							   int stepBlock ,
							   ImageType<Input> imageType )
	{
		if( stepBlock <= 0 )
			throw new IllegalArgumentException("stepBlock must be >= 1");

		this.imageType = imageType;

		gradient = createGradient(imageType);

		this.orientationBins = orientationBins;
		this.pixelsPerCell = pixelsPerCell;
		this.cellsPerBlock = cellsPerBlock;
		this.stepBlock = stepBlock;

		descriptions = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DescribeDenseHogAlg.this.orientationBins*
						DescribeDenseHogAlg.this.cellsPerBlock *DescribeDenseHogAlg.this.cellsPerBlock);
			}
		};

		computeWeightBlockPixels();
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
	 * Compute gaussian weights applied to each pixel in the block
	 */
	protected void computeWeightBlockPixels() {

		int pixelsPerBlock = cellsPerBlock * pixelsPerCell;

		Kernel2D_F64 kernel = FactoryKernelGaussian.gaussian2D_F64(0.5*pixelsPerBlock,
				pixelsPerBlock/2,pixelsPerBlock%2==1,false);
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
		orientation.reshape(input.width,input.height);
		magnitude.reshape(input.width,input.height);

		// pixel gradient
		gradient.process(input,derivX,derivY);

		computePixelFeatures();
	}

	/**
	 * Computes the orientation and magnitude of each pixel
	 */
	private void computePixelFeatures() {
		for (int y = 0; y < derivX.height; y++) {
			int pixelIndex = y*derivX.width;
			int endIndex = pixelIndex+derivX.width;
			for (; pixelIndex < endIndex; pixelIndex++ ) {
				float dx = derivX.data[pixelIndex];
				float dy = derivY.data[pixelIndex];

				// angle from 0 to pi radians
				orientation.data[pixelIndex] = UtilAngle.atanSafe(dy,dx) + GrlConstants.F_PId2;
				// gradient magnitude
				magnitude.data[pixelIndex] = Math.sqrt(dx*dx + dy*dy);
			}
		}
	}

	/**
	 * Computes the descriptor across the input image
	 */
	public void process() {
		locations.reset();
		descriptions.reset();

		int stepBlockPixelsX = pixelsPerCell *stepBlock;
		int stepBlockPixelsY = pixelsPerCell *stepBlock;

		int maxY = derivX.height - pixelsPerCell * cellsPerBlock + 1;
		int maxX = derivX.width - pixelsPerCell * cellsPerBlock + 1;

		for (int y = 0; y < maxY; y += stepBlockPixelsY ) {
			for (int x = 0; x < maxX; x += stepBlockPixelsX ) {
				TupleDesc_F64 d = descriptions.grow();
				Arrays.fill(d.value,0);
				histogram = d.value;

				for (int cellRow = 0; cellRow < cellsPerBlock; cellRow++) {
					int blockPixelRow = cellRow* pixelsPerCell;
					for (int cellCol = 0; cellCol < cellsPerBlock; cellCol++) {
						int blockPixelCol = cellCol* pixelsPerCell;

						computeCellHistogram(x+blockPixelCol, y+blockPixelRow, cellCol, cellRow);
					}
				}

				DescribeSiftCommon.normalizeDescriptor(d,0.2);
				locations.grow().set(x,y);
			}
		}
	}

	/**
	 * Computes the histogram for the block with the specified lower extent
	 * @param pixelX0 cell's lower extent x-axis in the image
	 * @param pixelY0 cell's lower extent y-axis in the image
	 * @param cellX Location of the cell in the block x-axis
	 * @param cellY Location of the cell in the block y-axis
	 */
	void computeCellHistogram(int pixelX0 , int pixelY0 ,
							  int cellX , int cellY ) {

		float angleBinSize = GrlConstants.F_PI/orientationBins;

		for (int i = 0; i < pixelsPerCell; i++) {
			int indexPixel = (pixelY0+i)*derivX.stride + pixelX0;
			int indexBlock = (cellY*pixelsPerCell+i)*pixelsPerCell*cellsPerBlock + cellX*pixelsPerCell;

			// Use center point of this cell to compute interpolation weights - bilinear interpolation
			double spatialWeightY0,spatialWeightY1,spatialWeightY2;

			if( i <= pixelsPerCell/2 ) {
				spatialWeightY1 = (i + pixelsPerCell/2.0)/pixelsPerCell;
				spatialWeightY0 = 1.0 - spatialWeightY1;
				spatialWeightY2 = 0;
			} else {
				spatialWeightY0 = 0;
				spatialWeightY2 = (i - pixelsPerCell/2.0)/pixelsPerCell;
				spatialWeightY1 = 1.0 - spatialWeightY2;
			}

			for (int j = 0; j < pixelsPerCell; j++, indexPixel++, indexBlock++ ) {
				// Use center point of this cell to compute interpolation weights - bilinear interpolation
				double spatialWeightX0,spatialWeightX1,spatialWeightX2;

				if( j <= pixelsPerCell/2 ) {
					spatialWeightX1 = (j + pixelsPerCell/2.0)/pixelsPerCell;
					spatialWeightX0 = 1.0 - spatialWeightX1;
					spatialWeightX2 = 0;
				} else {
					spatialWeightX0 = 0;
					spatialWeightX2 = (j - pixelsPerCell/2.0)/pixelsPerCell;
					spatialWeightX1 = 1.0 - spatialWeightX2;
				}

				// angle from 0 to pi radians
				float angle = this.orientation.data[indexPixel];

				// gradient magnitude
				double magnitude = this.magnitude.data[indexPixel];

				// Apply spatial weighting to magnitude
				magnitude *= this.weights[ indexBlock ];

				// Add the weighted gradient using linear interpolation to angle bins
				float findex0 = angle/angleBinSize;
				int index0 = (int)findex0;
				double oriWeight1 = findex0-index0;
				index0 %= orientationBins;
				int index1 = (index0+1)%orientationBins;

				// spatial bilinear interpolation + orientation linear interpolation
				// + gaussian weighting (previously applied)
				addToHistogram( cellX-1, cellY-1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX0*spatialWeightY0);
				addToHistogram( cellX-1, cellY-1 , index1, oriWeight1*magnitude*spatialWeightX0*spatialWeightY0);

				addToHistogram( cellX, cellY-1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX1*spatialWeightY0);
				addToHistogram( cellX, cellY-1 , index1, oriWeight1*magnitude*spatialWeightX1*spatialWeightY0);

				addToHistogram( cellX+1, cellY-1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX2*spatialWeightY0);
				addToHistogram( cellX+1, cellY-1 , index1, oriWeight1*magnitude*spatialWeightX2*spatialWeightY0);

				addToHistogram( cellX-1, cellY , index0, (1.0-oriWeight1)*magnitude*spatialWeightX0*spatialWeightY1);
				addToHistogram( cellX-1, cellY , index1, oriWeight1*magnitude*spatialWeightX0*spatialWeightY1);

				addToHistogram( cellX, cellY , index0, (1.0-oriWeight1)*magnitude*spatialWeightX1*spatialWeightY1);
				addToHistogram( cellX, cellY , index1, oriWeight1*magnitude*spatialWeightX1*spatialWeightY1);

				addToHistogram( cellX+1, cellY , index0, (1.0-oriWeight1)*magnitude*spatialWeightX2*spatialWeightY1);
				addToHistogram( cellX+1, cellY , index1, oriWeight1*magnitude*spatialWeightX2*spatialWeightY1);

				addToHistogram( cellX-1, cellY+1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX0*spatialWeightY2);
				addToHistogram( cellX-1, cellY+1 , index1, oriWeight1*magnitude*spatialWeightX0*spatialWeightY2);

				addToHistogram( cellX, cellY+1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX1*spatialWeightY2);
				addToHistogram( cellX, cellY+1 , index1, oriWeight1*magnitude*spatialWeightX1*spatialWeightY2);

				addToHistogram( cellX+1, cellY+1 , index0, (1.0-oriWeight1)*magnitude*spatialWeightX2*spatialWeightY2);
				addToHistogram( cellX+1, cellY+1 , index1, oriWeight1*magnitude*spatialWeightX2*spatialWeightY2);

			}
		}
	}

	/**
	 * Adds the magnitude to the histogram at the specified cell and orientation
	 * @param cellX cell coordinate
	 * @param cellY cell coordinate
	 * @param orientationIndex orientation coordinate
	 * @param magnitude edge magnitude
	 */
	private void addToHistogram(int cellX, int cellY, int orientationIndex, double magnitude) {
		// see if it's being applied to a valid cell in the histogram
		if( cellX < 0 || cellX >= cellsPerBlock)
			return;
		if( cellY < 0 || cellY >= cellsPerBlock)
			return;

		int index = (cellY*cellsPerBlock + cellX)*orientationBins + orientationIndex;
		histogram[index] += magnitude;
	}

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
	public int getRegionWidthPixel() {
		return pixelsPerCell * cellsPerBlock;
	}

	public void setPixelsPerCell(int pixelsPerCell) {
		this.pixelsPerCell = pixelsPerCell;
	}

	public int getPixelsPerCell() {
		return pixelsPerCell;
	}

	public int getCellsPerBlock() {
		return cellsPerBlock;
	}

	public int getStepBlock() {
		return stepBlock;
	}

	public int getOrientationBins() {
		return orientationBins;
	}

	public void setStepBlock(int stepBlock) {
		this.stepBlock = stepBlock;
	}

	public ImageType<Input> getImageType() {
		return imageType;
	}

	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(orientationBins* cellsPerBlock * cellsPerBlock);
	}
}
