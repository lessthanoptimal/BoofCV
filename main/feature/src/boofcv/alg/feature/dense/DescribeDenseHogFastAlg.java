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

import boofcv.abst.feature.dense.DescribeImageDenseHoG;
import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A variant on the original Histogram of Oriented Gradients (HOG) [1] in which spatial Gaussian weighting
 * has been omitted, allowing for cell histograms to be computed only once.  This results in about a
 * two times speed up.
 * </p>
 *
 * <p>For a description of standard HOG see {@link DescribeImageDenseHoG}.  Difference from standard HOG</p>
 * <ul>
 *     <li>No gaussian spatial weighting for each pixel inside a block</li>
 *     <li>No bilinear interpolation between cell centers</li>
 * </ul>
 *
 * <p>[1] Dalal, Navneet, and Bill Triggs. "Histograms of oriented gradients for human detection." Computer
 * Vision and Pattern Recognition, 2005. CVPR 2005.</p>
 *
 * @author Peter Abeles
 */
public class DescribeDenseHogFastAlg<Input extends ImageBase> extends BaseDenseHog<Input> {


	// storage for histograms in each cell
	Cell cells[] = new Cell[0];
	// number of cell rows and columns in the image
	int cellRows,cellCols;

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param pixelsPerCell Number of pixel's wide a cell is.  8 recommended
	 * @param cellsPerBlockX Number of cells's wide a block is. 3 recommended
	 * @param cellsPerBlockY Number of cells's wide a block is. 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public DescribeDenseHogFastAlg(int orientationBins , int pixelsPerCell , int cellsPerBlockX , int cellsPerBlockY,
								   int stepBlock ,
								   ImageType<Input> imageType )
	{
		super(orientationBins, pixelsPerCell, cellsPerBlockX, cellsPerBlockY, stepBlock, imageType);
	}

	/**
	 * Computes the descriptor across the input image
	 */
	@Override
	public void process() {
		locations.reset();
		descriptions.reset();

		// see if the cell array needs to grow for this image.  Recycle data when growing
		growCellArray(derivX.width, derivX.height);

		computeCellHistograms();

		int cellRowMax = (cellRows - (cellsPerBlockY -1));
		int cellColMax = (cellCols - (cellsPerBlockX -1));

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
		cellCols = imageWidth/ pixelsPerCell;
		cellRows = imageHeight/ pixelsPerCell;

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
		int gridX0 = (int)Math.ceil(pixelX0/(double) pixelsPerCell);
		int gridY0 = (int)Math.ceil(pixelY0/(double) pixelsPerCell);

		int gridX1 = pixelX1/ pixelsPerCell - cellsPerBlockX;
		int gridY1 = pixelY1/ pixelsPerCell - cellsPerBlockY;

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
		locations.grow().set(col* pixelsPerCell,row* pixelsPerCell);

		TupleDesc_F64 d = descriptions.grow();

		int indexDesc = 0;
		for (int i = 0; i < cellsPerBlockY; i++) {
			for (int j = 0; j < cellsPerBlockX; j++) {
				Cell c = cells[(row+i)*cellCols + (col+j)];

				for (int k = 0; k < c.histogram.length; k++) {
					d.value[indexDesc++] = c.histogram[k];
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

		int width = cellCols* pixelsPerCell;
		int height = cellRows* pixelsPerCell;

		float angleBinSize = GrlConstants.F_PI/orientationBins;

		int indexCell = 0;
		for (int i = 0; i < height; i += pixelsPerCell) {
			for (int j = 0; j < width; j += pixelsPerCell, indexCell++ ) {
				Cell c = cells[indexCell];
				c.reset();

				for (int k = 0; k < pixelsPerCell; k++) {
					int indexPixel = (i+k)*derivX.width+j;

					for (int l = 0; l < pixelsPerCell; l++, indexPixel++ ) {
						float pixelDX = this.derivX.data[indexPixel];
						float pixelDY = this.derivY.data[indexPixel];

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

	public int getCellRows() {
		return cellRows;
	}

	public int getCellCols() {
		return cellCols;
	}

	public Cell getCell( int row , int col ) {
		return cells[row*cellCols + col];
	}

	public static class Cell
	{
		public float histogram[];

		public void reset() {
			Arrays.fill(histogram,0);
		}
	}
}
