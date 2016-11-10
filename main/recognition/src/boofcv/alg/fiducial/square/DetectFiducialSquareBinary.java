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

package boofcv.alg.fiducial.square;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.util.Arrays;


/**
 * <p>
 * Square fiducial that encodes numerical values in a binary N by N grids, where N &ge; 3.  The outer border
 * is entirely black while the inner portion is divided into a grid of equally sized back and white squares.
 * Typical grid sizes are 3x3, 4x4, and 5x5, which can encode up to 32, 4096, and 2,097,152 unique values respectively.
 * In other words, a grid of size N can encode N*N-4 bits, or a number with 2<sup>N*N-4</sup> values.
 * The lower left corner is always back and while all the other corners are always white.
 * This allows orientation to be uniquely determined.
 * </p>
 * <center>
 * <img src="doc-files/square_binary.png"/>
 * </center>
 * <p>
 * The above image is an example of a 4x4 grid and visually shows the fiducials internal coordinate system.
 * The center of the fiducial is the origin  of the coordinate system, e.g. all sides are width/2 distance
 * away from the origin.  +x is to the right, +y is up, and +z out of the paper towards the viewer.
 * The black orientation corner is pointed out in the above image.
 * The fiducial's width refers to the width of each side along the black border NOT the internal encoded image.
 * The size of each square is the same and has a width of (fiducal width)*(1.0 - 2.0*(border fractional width))/N.
 * </p>
 * <p>
 * NOTE: While a larger grid size will allow you to encode more numbers it will increase the rate at which ID numbers
 * are incorrectly identified.<br>
 * NOTE: The size of the border can be adjusted, but 0.25 is recommended.  The thinner the black border is the worse
 * it will perform when viewed at an angle.  However, the closer the fiducial is the less this is an issue allowing
 * for thinner borders.
 * <p>
 *
 * @author Peter Abeles Original author/maintainer
 * @author Nathan Pahucki  Added the ability to use more than 4x4 grid for Capta360, <a href="mailto:npahucki@gmail.com"> npahucki@gmail.com</a>
 */
public class DetectFiducialSquareBinary<T extends ImageGray>
		extends BaseDetectFiducialSquare<T> {

	// helper data structures for computing the value of each grid element
	int[] counts, classified, tmp;

	// converts the input image into a binary one
	private GrayU8 binaryInner = new GrayU8(1,1);
	// storage for no border sub-image
	private GrayF32 grayNoBorder = new GrayF32();

	// number of rows/columns in the encoded binary pattern
	private int gridWidth;

	// width of a square in the inner undistorted image.
	protected final static int w=10;
	// total number of pixels in a square.  Outer pixels are ignored, hence -2 for each axis
	protected final static int N=(w-2)*(w-2);

	// length of a side for the fiducial's black border in world units.
	private double lengthSide = 1;

	// ambiguity threshold. 0 to 1.  0 = very strict and 1 = anything goes
	// Sets how strict a square must be black or white for it to be accepted.
	double ambiguityThreshold = 0.4;

	/**
	 * Configures the fiducial detector
	 *
	 * @param gridWidth Number of elements wide the encoded square grid is. 3,4, or 5 is recommended.
	 * @param borderWidthFraction Fraction of the fiducial's width that the border occupies. 0.25 is recommended.
	 * @param inputToBinary Converts the input image into a binary image
	 * @param quadDetector Detects quadrilaterals in the input image
	 * @param inputType Type of image it's processing
	 */
	public DetectFiducialSquareBinary(int gridWidth,
									  double borderWidthFraction ,
									  double minimumBlackBorderFraction ,
									  final InputToBinary<T> inputToBinary,
									  final BinaryPolygonDetector<T> quadDetector, Class<T> inputType) {
		// Black borders occupies 2.0*borderWidthFraction of the total width
		// The number of pixels for each square is held constant and the total pixels for the inner region
		// is determined by the size of the grid
		// The number of pixels in the undistorted image (squarePixels) is selected using the above information
		super(inputToBinary,quadDetector,borderWidthFraction,minimumBlackBorderFraction,
				(int)Math.round((w * gridWidth) /(1.0-borderWidthFraction*2.0)) ,inputType);

		if( gridWidth < 3 || gridWidth > 8)
			throw new IllegalArgumentException("The grid must be at least 3 and at most 8 elements wide");

		this.gridWidth = gridWidth;
		binaryInner.reshape(w * gridWidth,w * gridWidth);
		counts = new int[getTotalGridElements()];
		classified = new int[getTotalGridElements()];
		tmp = new int[getTotalGridElements()];
	}

	@Override
	protected boolean processSquare(GrayF32 gray, Result result, double edgeInside, double edgeOutside) {
		int off = (gray.width - binaryInner.width) / 2;
		gray.subimage(off, off, off + binaryInner.width, off + binaryInner.width, grayNoBorder);

		// convert input image into binary number
		double threshold = (edgeInside+edgeOutside)/2;
		findBitCounts(grayNoBorder,threshold);

		if (thresholdBinaryNumber()) {
			if( verbose ) System.out.println("  can't threshold binary, ambiguous");
			return false;
		}
		
		// adjust the orientation until the black corner is in the lower left
		if (rotateUntilInLowerCorner(result)) {
			if( verbose ) System.out.println("  rotate to corner failed");
			return false;
		}

		result.which = extractNumeral();
		result.lengthSide = lengthSide;

		//printClassified();
		return true;
	}

	/**
	 * Extract the numerical value it encodes
	 * @return the int value of the numeral.
	 */
	protected int extractNumeral() {
		int val = 0;
		final int topLeft = getTotalGridElements() - gridWidth;
		int shift = 0;

		// -2 because the top and bottom rows have 2 unusable bits (the first and last)
		for(int i = 1; i < gridWidth - 1; i++) {
			final int idx = topLeft + i;
			val |= classified[idx] << shift;
			//System.out.println("val |= classified[" + idx + "] << " + shift + ";");
			shift++;
		}

		// Don't do the first or last row, handled above and below - special cases
		for(int ii = 1; ii < gridWidth - 1; ii++) {
			for(int i = 0; i < gridWidth; i++) {
				final int idx = getTotalGridElements() - (gridWidth * (ii + 1)) + i;
				val |= classified[idx] << shift;
				//  System.out.println("val |= classified[" + idx + "] << " + shift + ";");
				shift++;
			}
		}

		// The last row
		for(int i = 1; i < gridWidth - 1; i++) {
			val |= classified[i] << shift;
			//System.out.println("val |= classified[" + i + "] << " + shift + ";");
			shift++;
		}

		return val;
	}

	/**
	 * Rotate the pattern until the black corner is in the lower right.  Sanity check to make
	 * sure there is only one black corner
	 */
	private boolean rotateUntilInLowerCorner(Result result) {
		// sanity check corners.  There should only be one exactly one black
		final int topLeft = getTotalGridElements() - gridWidth;
		final int topRight = getTotalGridElements() - 1;
		final int bottomLeft = 0;
		final int bottomRight = gridWidth - 1;

		if (classified[bottomLeft] + classified[bottomRight] + classified[topRight] + classified[topLeft] != 1)
			return true;

		// Rotate until the black corner is in the lower left hand corner on the image.
		// remember that origin is the top left corner
		result.rotation = 0;
		while (classified[topLeft] != 1) {
			result.rotation++;
			rotateClockWise();
		}
		return false;
	}

	protected void rotateClockWise() {

		final int totalElements = getTotalGridElements();

		// Swap the four corners
		for (int ii = 0; ii < gridWidth; ii++) {
			for (int i = 0; i < gridWidth; i++) {
				final int fromIdx = ii * gridWidth + i;
				final int toIdx = (totalElements - (gridWidth * (i + 1))) + ii;
				tmp[fromIdx] = classified[toIdx];
			}
		}

		System.arraycopy(tmp, 0, classified, 0, totalElements);
	}


	/**
	 * Sees how many pixels were positive and negative in each square region.  Then decides if they
	 * should be 0 or 1 or unknown
	 */
	protected boolean thresholdBinaryNumber() {

		int lower = (int) (N * (ambiguityThreshold / 2.0));
		int upper = (int) (N * (1 - ambiguityThreshold / 2.0));

		final int totalElements = getTotalGridElements();
		for (int i = 0; i < totalElements; i++) {
			if (counts[i] < lower) {
				classified[i] = 0;
			} else if (counts[i] > upper) {
				classified[i] = 1;
			} else {
				// it's ambiguous so just fail
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the gray scale image into a binary number.  Skip the outer 1 pixel of each inner square.  These
	 * tend to be incorrectly classified due to distortion.
	 */
	protected void findBitCounts(GrayF32 gray , double threshold ) {
		// compute binary image using an adaptive algorithm to handle shadows
		ThresholdImageOps.threshold(gray,binaryInner,(float)threshold,true);

		Arrays.fill(counts, 0);
		for (int row = 0; row < gridWidth; row++) {
			int y0 = row * binaryInner.width / gridWidth + 1;
			int y1 = (row + 1) * binaryInner.width / gridWidth - 1;
			for (int col = 0; col < gridWidth; col++) {
				int x0 = col * binaryInner.width / gridWidth + 1;
				int x1 = (col + 1) * binaryInner.width / gridWidth - 1;

				int total = 0;
				for (int i = y0; i < y1; i++) {
					int index = i * binaryInner.width + x0;
					for (int j = x0; j < x1; j++) {
						total += binaryInner.data[index++];
					}
				}

				counts[row * gridWidth + col] = total;
			}
		}
	}

	public void setLengthSide(final double lengthSide) {
		this.lengthSide = lengthSide;
	}

	/**
	 * Number of elements wide the grid is
	 */
	public int getGridWidth() {
		return gridWidth;
	}

	/**
	 * parameters which specifies how tolerant it is of a square being ambiguous black or white.
	 * @param ambiguityThreshold 0 to 1, inclusive
	 */
	public void setAmbiguityThreshold(double ambiguityThreshold) {
		if( ambiguityThreshold < 0 || ambiguityThreshold > 1 )
			throw new IllegalArgumentException("Must be from 0 to 1, inclusive");
		this.ambiguityThreshold = ambiguityThreshold;
	}

	/**
	 * Total number of elements in the grid
	 */
	private int getTotalGridElements() {
		return gridWidth * gridWidth;
	}

	public long getNumberOfDistinctFiducials() {
		// The -4 is for the 4 orientation squares
		return (long) Math.pow(2, gridWidth * gridWidth - 4);
	}

	// For troubleshooting.
	public GrayF32 getGrayNoBorder() { return grayNoBorder; }

	public GrayU8 getBinaryInner() {
		return binaryInner;
	}

	// This is only works well as a visual representation if the output font is mono spaced.
	public void printClassified() {
		System.out.println();
		System.out.println("      ");
		for (int row = 0; row < gridWidth; row++) {
			System.out.print(" ");
			for (int col = 0; col < gridWidth; col++) {
				System.out.print(classified[row * gridWidth + col] == 1 ? " " : "X");
			}
			System.out.print(" ");
			System.out.println();
		}
		System.out.println("      ");

	}
}

