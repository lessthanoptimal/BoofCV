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

package boofcv.alg.fiducial.square;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;

/**
 * This detector decodes binary square fiducials where markers are indentified from a set of markers which is much
 * smaller than the number of possible numbers in the grid. The ID of each marker is designed to be orthogonal from
 * the others. Error correction is performed by taking the decoded value and finding the marker ID with the smallest
 * number of bit errors (hamming distance). Orientation is determined by rotating the decoded array while searching
 * for the best fit. Markers in this family include ArUco, AprilTag, ArToolKit+, and ARTAG.
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareHamming<T extends ImageGray<T>>
		extends BaseDetectFiducialSquare<T> {
	/** Binary pattern for each known marker */
	@Getter @Setter public final DogArray<DogArray_B> markers = new DogArray<>(DogArray_B::new);

	/** Maximum allowed hamming distance from best fit marker */
	@Getter @Setter public int maximumBitError = 0;

	// number of rows/columns in the encoded binary pattern
	@Getter protected int gridWidth;

	// converts the input image into a binary one
	private GrayU8 binaryInner = new GrayU8(1,1);
	// storage for no border sub-image
	private GrayF32 grayNoBorder = new GrayF32();

	// width of a square in the inner undistorted image.
	protected final static int w=10;
	// total number of pixels in a square. Outer pixels are ignored, hence -2 for each axis
	protected final static int N=(w-4)*(w-4);

	/**
	 * Configures the fiducial detector
	 *
	 * @param gridWidth Number of elements wide the encoded square grid is. 3,4, or 5 is recommended.
	 * @param borderWidthFraction Fraction of the fiducial's width that the border occupies. 0.25 is recommended.
	 * @param inputToBinary Converts the input image into a binary image
	 * @param quadDetector Detects quadrilaterals in the input image
	 * @param inputType Type of image it's processing
	 */
	public DetectFiducialSquareHamming( int gridWidth,
										double borderWidthFraction,
										double minimumBlackBorderFraction,
										final InputToBinary<T> inputToBinary,
										final DetectPolygonBinaryGrayRefine<T> quadDetector, Class<T> inputType ) {
		// Black borders occupies 2.0*borderWidthFraction of the total width
		// The number of pixels for each square is held constant and the total pixels for the inner region
		// is determined by the size of the grid
		// The number of pixels in the undistorted image (squarePixels) is selected using the above information
		super(inputToBinary, quadDetector, false,
				borderWidthFraction, minimumBlackBorderFraction, (int)Math.round((w*gridWidth)/(1.0 - borderWidthFraction*2.0)),
				inputType);

		if (gridWidth < 3 || gridWidth > 8)
			throw new IllegalArgumentException("The grid must be at least 3 and at most 8 elements wide");

		this.gridWidth = gridWidth;
		binaryInner.reshape(w*gridWidth, w*gridWidth);
	}

	public void configureGrid( int gridWidth, double borderFraction ) {

	}

	@Override protected boolean processSquare( GrayF32 square, Result result, double edgeInside, double edgeOutside ) {
		// convert each cell in the grid into a binary value by voting

		return false;
	}

	public static class Detected {
		/** Which marker matched the observation */
		public int markerIndex;
		/** The hamming distance of the detection relative to the pattern */
		public int hammingDistance;
	}

	public static class Marker {
		/** Expected binary pattern for this marker */
		public final DogArray_B pattern = new DogArray_B();
		/** Unique ID assigned to this marker */
		public long id;
	}
}
