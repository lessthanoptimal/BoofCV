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
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.fiducial.qrcode.PackedBits32;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import lombok.Setter;

/**
 * This detector decodes binary square fiducials where markers are indentified from a set of markers which is much
 * smaller than the number of possible numbers in the grid. The ID of each marker is designed to be orthogonal from
 * the others. Error correction is performed by taking the decoded value and finding the marker ID with the smallest
 * number of bit errors (hamming distance). Orientation is determined by rotating the decoded array while searching
 * for the best fit. Markers in this family include ArUco, AprilTag, ArToolKit+, and ARTAG.
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareHamming<T extends ImageGray<T>> extends BaseDetectFiducialSquare<T> {
	// IDEA: See if it's rectangle is too small for there to be any chance that it could resolve X number of bits
	//       and it should just ignore the detection.

	/** Describes the marker it looks for */
	@Getter public final ConfigHammingMarker description;
	/** converts the input image into a binary one */
	@Getter protected final GrayU8 binaryInner = new GrayU8(1, 1);
	/** storage for no border sub-image */
	@Getter protected final GrayF32 grayNoBorder = new GrayF32();
	/** How much ambiguous bits increase the error by. 0 = no penalty. 1=simple addition. */
	@Getter @Setter public double ambiguousPenaltyFrac = 0.5;

	// width of a square in the inner undistorted image.
	protected final static int w = 10;
	// total number of pixels in a square. Outer pixels are ignored, hence -2 for each axis
	protected final static int N = (w - 4)*(w - 4);

	// The read in bits in a format the codec can understand
	final PackedBits32 bits = new PackedBits32();
	// Storage the bits inside an image so that it can be rotated easily
	final GrayU8 bitImage = new GrayU8(1, 1);
	// Stores intermediate results when rotating
	final GrayU8 workImage = new GrayU8(1, 1);

	// how many bits are not obvious 0 or 1
	int ambiguousBitCount = 0;

	/**
	 * Configures the fiducial detector
	 *
	 * @param inputToBinary Converts the input image into a binary image
	 * @param quadDetector Detects quadrilaterals in the input image
	 * @param inputType Type of image it's processing
	 */
	public DetectFiducialSquareHamming( ConfigHammingMarker description,
										double minimumBlackBorderFraction,
										final InputToBinary<T> inputToBinary,
										final DetectPolygonBinaryGrayRefine<T> quadDetector, Class<T> inputType ) {
		// Black borders occupies 2.0*borderWidthFraction of the total width
		// The number of pixels for each square is held constant and the total pixels for the inner region
		// is determined by the size of the grid
		// The number of pixels in the undistorted image (squarePixels) is selected using the above information
		super(inputToBinary, quadDetector, false,
				description.borderWidthFraction, minimumBlackBorderFraction,
				(int)Math.round((w*description.gridWidth)/(1.0 - description.borderWidthFraction*2.0)),
				inputType);
		this.description = description;

		binaryInner.reshape(w*description.gridWidth, w*description.gridWidth);
		bitImage.reshape(description.gridWidth, description.gridWidth);
	}

	@Override protected boolean processSquare( GrayF32 square, Result result, double edgeInside, double edgeOutside ) {
		int off = (square.width - binaryInner.width)/2;
		square.subimage(off, off, off + binaryInner.width, off + binaryInner.width, grayNoBorder);

		// convert input image into binary number
		double threshold = (edgeInside + edgeOutside)/2;
		int errorPureColor = decodeDataBits(grayNoBorder, threshold);

		if (verbose != null) verbose.printf("_ square: threshold=%.1f ambiguous=%d errorPure=%d",
				threshold, ambiguousBitCount, errorPureColor);

		if (errorPureColor == 0) {
			if (verbose != null) verbose.println();
			return false;
		}

		// Search all markers and orientation to see what is the best match. Stop if it finds a perfect match.
		int bestMarker = -1;
		int bestOrientation = -1;
		int bestError = Integer.MAX_VALUE;
		for (int orientation = 0; orientation < 4 && bestError != 0; orientation++) {
			// git bit array for this orientation
			convertBitImageToBitArray();
			final int numWords = bits.arrayLength();

			// Go through all markers
			for (int markerIdx = 0; markerIdx < description.encoding.size(); markerIdx++) {
				int[] data = description.encoding.get(markerIdx).pattern.data;

				int error = 0;
				for (int wordIdx = 0; wordIdx < numWords; wordIdx++) {
					error += DescriptorDistance.hamming(bits.data[wordIdx] ^ data[wordIdx]);
				}

				// Check to see if this is the best result
				if (error < bestError) {
					bestError = error;
					bestOrientation = orientation;
					bestMarker = markerIdx;

					// stop if it's perfect
					if (bestError == 0)
						break;
				}
			}

			// Rotate image and repeat
			ImageMiscOps.rotateCW(bitImage, workImage);
			bitImage.setTo(workImage);
		}

		if (verbose != null) verbose.printf(" hamming_error=%d minimum=%d", bestError, description.minimumHamming);

		// See if the error is too large or worse than a square that's pure white or back. This is to reduce false
		// positives. Also consider ambiguous bits, as they are much more likely to be pure noise
		if (bestError + ambiguousBitCount*ambiguousPenaltyFrac > description.minimumHamming ||
				bestError >= errorPureColor) {
			if (verbose != null) verbose.println();
			return false;
		}

		// save the results
		result.which = bestMarker;
		result.lengthSide = 1;
		result.rotation = bestOrientation;
		result.error = bestError;

		if (verbose != null) verbose.printf(" id=%d orientation=%d\n", result.which, result.rotation);

		return true;
	}

	/**
	 * Converts the binary image into a dense bit array that's understood by the codec
	 */
	void convertBitImageToBitArray() {
		bits.resize(bitImage.width*bitImage.height);
		for (int y = 0, i = 0; y < bitImage.height; y++) {
			for (int x = 0; x < bitImage.width; x++, i++) {
				bits.set(bits.size - i - 1, bitImage.data[i]);
			}
		}
	}

	/**
	 * Converts the gray scale image into a binary number. Skip the outer 1 pixel of each inner square. These
	 * tend to be incorrectly classified due to distortion.
	 *
	 * @return The error relative to a pure white or black square. The best score must be able to beat this.
	 */
	protected int decodeDataBits( GrayF32 gray, double threshold ) {
		// compute binary image using an adaptive algorithm to handle shadows
		ThresholdImageOps.threshold(gray, binaryInner, (float)threshold, true);

		final int voteThreshold = N/2;
		final int ambiguousThreshold = N/4;
		final int gridWidth = description.gridWidth;
		ambiguousBitCount = 0;

		int countOnes = 0;
		for (int row = 0; row < gridWidth; row++) {
			int y0 = row*binaryInner.width/gridWidth + 2;
			int y1 = (row + 1)*binaryInner.width/gridWidth - 2;
			for (int col = 0; col < gridWidth; col++) {
				int x0 = col*binaryInner.width/gridWidth + 2;
				int x1 = (col + 1)*binaryInner.width/gridWidth - 2;

				int total = 0;
				for (int i = y0; i < y1; i++) {
					int index = i*binaryInner.width + x0;
					for (int j = x0; j < x1; j++) {
						total += binaryInner.data[index++];
					}
				}

				// See if this bit is not clearly white or black
				if ((total > voteThreshold ? N - total : total) >= ambiguousThreshold)
					ambiguousBitCount++;

				int bit = total <= voteThreshold ? 1 : 0;
				bitImage.data[row*gridWidth + col] = (byte)bit;
				countOnes += bit;
			}
		}

		// return best score if you assume it is pure black or white square inside
		return Math.min(countOnes, gridWidth*gridWidth - countOnes);
	}
}
