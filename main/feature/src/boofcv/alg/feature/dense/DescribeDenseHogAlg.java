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

import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import org.ddogleg.stats.UtilGaussian;

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
public class DescribeDenseHogAlg<Input extends ImageBase> extends BaseDenseHog<Input> {

	// orientation and magnitude of each pixel
	protected GrayF32 orientation = new GrayF32(1,1);
	protected GrayF64 magnitude = new GrayF64(1,1); // stored as F64 instead of F32 for speed

	// the active histogram being worked on
	double histogram[];

	// spatial weights applied to each in a block
	// stored in a row major order
	double weights[];

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param pixelsPerCell Number of pixel's wide a cell is.  8 recommended
	 * @param cellsPerBlockX Number of cells's wide a block is. x-axis 3 recommended
	 * @param cellsPerBlockY Number of cells's wide a block is. x-axis 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public DescribeDenseHogAlg(int orientationBins , int pixelsPerCell ,
							   int cellsPerBlockX , int cellsPerBlockY,
							   int stepBlock ,
							   ImageType<Input> imageType )
	{
		super(orientationBins, pixelsPerCell, cellsPerBlockX, cellsPerBlockY, stepBlock, imageType);

		computeWeightBlockPixels();
	}

	/**
	 * Compute gaussian weights applied to each pixel in the block
	 */
	protected void computeWeightBlockPixels() {

		int rows = cellsPerBlockY*pixelsPerCell;
		int cols = cellsPerBlockX*pixelsPerCell;

		weights = new double[ rows*cols ];

		double offsetRow=0,offsetCol=0;
		int radiusRow=rows/2,radiusCol=cols/2;
		if( rows%2 == 0 ) {
			offsetRow = 0.5;
		}
		if( cols%2 == 0 ) {
			offsetCol = 0.5;
		}

		// use linear seperability of a Gaussian to make computation easier
		// sigma is 1/2 the width along each axis
		int index = 0;
		for (int row = 0; row < rows; row++) {
			double drow = row-radiusRow+offsetRow;
			double pdfRow = UtilGaussian.computePDF(0, radiusRow, drow);

			for (int col = 0; col < cols; col++) {
				double dcol = col-radiusCol+offsetCol;
				double pdfCol = UtilGaussian.computePDF(0, radiusCol, dcol);

				weights[index++] = pdfCol*pdfRow;
			}
		}
		// normalize so that the largest value is 1.0
		double max = 0;
		for (int i = 0; i < weights.length; i++) {
			if( weights[i] > max ) {
				max = weights[i];
			}
		}
		for (int i = 0; i < weights.length; i++) {
			weights[i] /= max;
		}
	}

	/**
	 * Specifies input image.  Gradient is computed immediately
	 * @param input input image
	 */
	@Override
	public void setInput( Input input ) {
		super.setInput(input);
		orientation.reshape(input.width,input.height);
		magnitude.reshape(input.width,input.height);

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
	@Override
	public void process() {
		locations.reset();
		descriptions.reset();

		int stepBlockPixelsX = pixelsPerCell *stepBlock;
		int stepBlockPixelsY = pixelsPerCell *stepBlock;

		int maxY = derivX.height - pixelsPerCell * cellsPerBlockY + 1;
		int maxX = derivX.width - pixelsPerCell * cellsPerBlockX + 1;

		for (int y = 0; y < maxY; y += stepBlockPixelsY ) {
			for (int x = 0; x < maxX; x += stepBlockPixelsX ) {
				TupleDesc_F64 d = descriptions.grow();
				Arrays.fill(d.value,0);
				histogram = d.value;

				for (int cellRow = 0; cellRow < cellsPerBlockY; cellRow++) {
					int blockPixelRow = cellRow* pixelsPerCell;
					for (int cellCol = 0; cellCol < cellsPerBlockX; cellCol++) {
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
			int indexBlock = (cellY*pixelsPerCell+i)*pixelsPerCell*cellsPerBlockX + cellX*pixelsPerCell;

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
	void addToHistogram(int cellX, int cellY, int orientationIndex, double magnitude) {
		// see if it's being applied to a valid cell in the histogram
		if( cellX < 0 || cellX >= cellsPerBlockX)
			return;
		if( cellY < 0 || cellY >= cellsPerBlockY)
			return;

		int index = (cellY*cellsPerBlockX + cellX)*orientationBins + orientationIndex;
		histogram[index] += magnitude;
	}
}
