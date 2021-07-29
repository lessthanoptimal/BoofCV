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

package boofcv.alg.fiducial.calib.chessbits;

import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.struct.GridShape;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO document
 *
 * @author Peter Abeles
 */
public class ChessBitsUtils {
	/** Fraction of a cell's length the data bit is */
	public @Getter @Setter double dataBitWidthFraction = 0.7;

	/** Fraction of the length the quite zone is around data bits */
	public @Getter @Setter double dataBorderFraction = 0.15;

	/** Shape of all possible markers */
	public final List<GridShape> markers = new ArrayList<>();

	/** Number of pixels it will reach for each bit when constructing binary image */
	public int pixelsPerBit = 2;

	/**
	 * Number of samples it will do for each bit. Each sample is one vote. Dynamically computed from {@link #pixelsPerBit}
	 */
	public int bitSampleCount;

	/** Used to encode and decode bit streams with coordinate information */
	public final ChessboardReedSolomonCodec codec = new ChessboardReedSolomonCodec();

	// Used to compute the homography from square coordinates into image pixels
	HomographyDirectLinearTransform dlt = new HomographyDirectLinearTransform(true);
	DogArray<AssociatedPair> storagePairs2D = new DogArray<>(AssociatedPair::new);
	// homography that describes the transform from bit-square (0 to 1.0) to image pixels
	DMatrixRMaj squareToPixel = new DMatrixRMaj(3, 3);

	// pre-allcoated workspace
	Rectangle2D_F64 rect = new Rectangle2D_F64();
	Point2D_F64 bitSquare = new Point2D_F64();

	/**
	 * Adds a new marker to the list
	 *
	 * @param rows Number of rows in the chessboard pattern
	 * @param cols Number of columns in thechessboard pattern
	 */
	public void addMarker( int rows, int cols ) {
		markers.add(new GridShape(rows, cols));
	}

	/**
	 * Call after its done being configured so that it can precompute everything that's needed
	 */
	public void fixate() {
		int N = findLargestCellCount();
		codec.configure(markers.size(), N);

		bitSampleCount = pixelsPerBit*2+1;
	}

	/**
	 * Returns the number of cells in the largest marker
	 */
	int findLargestCellCount() {
		int largest = 0;
		for (int i = 0; i < markers.size(); i++) {
			GridShape g = markers.get(i);
			int n = g.cols*g.rows;
			if (n > largest)
				largest = n;
		}
		return largest;
	}

	/**
	 * Returns the rectangle for a specific data bit in bit-square coordinates
	 *
	 * @param row Bit's row
	 * @param col Bit's column
	 * @param rect Rectangle containing the bit
	 */
	public void bitRect( int row, int col, Rectangle2D_F64 rect ) {
		int bitGridLength = codec.getGridBitLength();

		double squareWidth = (1.0 - dataBorderFraction*2)/bitGridLength;
		double bitPadding = squareWidth*(1.0 - dataBitWidthFraction)/2.0;

		rect.p0.x = dataBorderFraction + col*squareWidth + bitPadding;
		rect.p0.y = dataBorderFraction + row*squareWidth + bitPadding;
		rect.p1.x = rect.p0.x + squareWidth*dataBitWidthFraction;
		rect.p1.y = rect.p0.y + squareWidth*dataBitWidthFraction;
	}

	/**
	 * Finds the correspondence from bit-square coordinates to image pixels
	 *
	 * @param a Pixel corresponding to (0,0)
	 * @param b Pixel corresponding to (w,0)
	 * @param c Pixel corresponding to (w,w)
	 * @param d Pixel corresponding to (0,w)
	 */
	public boolean computeGridToImage( Point2D_F64 a, Point2D_F64 b, Point2D_F64 c, Point2D_F64 d ) {
		storagePairs2D.resetResize(4);
		storagePairs2D.get(0).setTo(0, 0, a.x, a.y);
		storagePairs2D.get(1).setTo(1, 0, b.x, b.y);
		storagePairs2D.get(2).setTo(1, 1, c.x, c.y);
		storagePairs2D.get(3).setTo(0, 1, d.x, d.y);

		return dlt.process(storagePairs2D.toList(), squareToPixel);
	}

	/**
	 * Selects pixels that it should sample for each bit. The number of pixels per bit is specified by pixelsPerBit.
	 * The order of bits is in row-major format with a block of size bitSampleCount. Points are sampled in a grid
	 * pattern with one points always in the center. This means there will be an odd number of points preventing a tie.
	 *
	 * @param pixels Image pixels that correspond pixels in binary version of grid
	 */
	public void selectPixelsToSample( DogArray<Point2D_F64> pixels ) {
		int bitGridLength = codec.getGridBitLength();

		pixels.reset();

		// size of the square
		double squareWidth = (1.0 - dataBorderFraction*2)/bitGridLength;

		// Size of the black square that's a bit
		double bitPadding = squareWidth*(1.0 - dataBitWidthFraction)/(pixelsPerBit*2);

		for (int row = 0; row < bitGridLength; row++) {
			for (int col = 0; col < bitGridLength; col++) {
				bitRect(row, col, rect);

				for (int i = 0; i < pixelsPerBit; i++) {
					// sample the inner region to avoid edge conditions on the boundary of white/black
					bitSquare.y = (rect.p1.y - rect.p0.y)*i/pixelsPerBit + rect.p0.y + bitPadding;
					for (int j = 0; j < pixelsPerBit; j++) {
						bitSquare.x = (rect.p1.x - rect.p0.x)*j/pixelsPerBit + rect.p0.x + bitPadding;

						GeometryMath_F64.mult(squareToPixel, bitSquare, pixels.grow());
					}
				}

				// Sample the exact center
				bitSquare.y = (rect.p1.y + rect.p0.y)*0.5;
				bitSquare.x = (rect.p1.x + rect.p0.x)*0.5;
				GeometryMath_F64.mult(squareToPixel, bitSquare, pixels.grow());
			}
		}
	}
}
