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
import boofcv.struct.GridCoordinate;
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
 * <p>Common functions that are needed for encoding, decoding, and detecting.</p>
 *
 * Definitions:
 * <dl>
 *     <dt>data region</dt>
 *     <dd>Square region containing the encoded message inside a white inner square</dd>
 *
 *     <dt>data region coordinates</dt>
 *     <dd>2D coordinate system with the origin in the data region's top-left corner. Values vary from 0 to 1. Where
 *     0 is at the origin and 1 is either the x or y axis border.</dd>
 *
 *     <dt>inner square</dt>
 *     <dd>Square (white or black) inside chessboard which does not touch the border</dd>
 *
 *     <dt>bit-cell</dt>
 *     <dd>Region in which a single bit of data is encoded. size is data-region's length / grid size</dd>
 *
 *     <dt>grid size</dt>
 *     <dd>The length of a grid. size=5 then there are 25 cells in the grid.</dd>
 *
 *     <dt>quite-zone</dt>
 *     <dd>White space surrounding an image feature which reduces confusion with the background or other features.</dd>
 * </dl>
 *
 * @author Peter Abeles
 */
public class ChessBitsUtils {
	/** Fraction of a bit-cell's length that the black square is drawn in */
	public @Getter @Setter double dataBitWidthFraction = 0.7;

	/** Fraction length of the quite-zone around data bits */
	public @Getter @Setter double dataBorderFraction = 0.15;

	/** Shape of all possible markers */
	public final List<GridShape> markers = new ArrayList<>();

	/** Length of the row/column in the grid it will sample when determining a cell's bit value */
	public int bitSampleGridSize = 2;

	/**
	 * Number of samples it will do for each bit. Each sample is one vote. Dynamically computed from {@link #bitSampleGridSize}
	 */
	@Getter protected int bitSampleCount;

	/** Used to encode and decode bit streams with coordinate information */
	public final ChessboardReedSolomonCodec codec = new ChessboardReedSolomonCodec();

	// Used to compute the homography from square coordinates into image pixels
	HomographyDirectLinearTransform dlt = new HomographyDirectLinearTransform(true);
	DogArray<AssociatedPair> storagePairs2D = new DogArray<>(AssociatedPair::new);
	// homography that describes the transform from data-region (0 to 1.0) to image pixels
	DMatrixRMaj squareToPixel = new DMatrixRMaj(3, 3);

	// pre-allcoated workspace
	Rectangle2D_F64 rect = new Rectangle2D_F64();
	Point2D_F64 bitSquare = new Point2D_F64();

	/**
	 * Adds a new marker to the list
	 *
	 * @param squareRows Number of square rows in the chessboard pattern
	 * @param squareCols Number of square columns in the chessboard pattern
	 */
	public void addMarker( int squareRows, int squareCols ) {
		markers.add(new GridShape(squareRows, squareCols));
	}

	/**
	 * Call after its done being configured so that it can precompute everything that's needed
	 */
	public void fixate() {
		int N = findLargestCellCount();
		codec.configure(markers.size(), N);

		bitSampleCount = bitSampleGridSize*2 + 1;
	}

	public void checkFixate() {
		if (bitSampleCount == 0)
			throw new RuntimeException("BUG you forgot to call fixate()");
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
	 * Returns the rectangle in which a data-bit will be written to. The rectangle will be in data-region
	 * coordinates.
	 *
	 * @param row Bit's row
	 * @param col Bit's column
	 * @param rect (Output) Rectangle containing the bit
	 */
	public void bitRect( int row, int col, Rectangle2D_F64 rect ) {
		int bitGridLength = codec.getGridBitLength();

		// How wide the square cell is that stores a bit + bit padding
		double cellWidth = (1.0 - dataBorderFraction*2)/bitGridLength;
		double offset = dataBorderFraction + cellWidth*(1.0 - dataBitWidthFraction)/2.0;

		rect.p0.x = col*cellWidth + offset;
		rect.p0.y = row*cellWidth + offset;
		rect.p1.x = rect.p0.x + cellWidth*dataBitWidthFraction;
		rect.p1.y = rect.p0.y + cellWidth*dataBitWidthFraction;
	}

	/**
	 * Finds the correspondence from data-region coordinates to image pixels
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
	 * pattern with one point in the center. This means there will be an odd number of points preventing a tie.
	 *
	 * @param pixels Image pixels that correspond pixels in binary version of grid
	 */
	public void selectPixelsToSample( DogArray<Point2D_F64> pixels ) {
		int bitGridLength = codec.getGridBitLength();

		pixels.reset();

		// sample the inner 50% of the data square
		double sampleLength = 0.5*dataBitWidthFraction;
		// Offset from the sides. This will center the sampling
		double padding = (1.0 - sampleLength)/2.0;

		for (int row = 0; row < bitGridLength; row++) {
			for (int col = 0; col < bitGridLength; col++) {
				bitRect(row, col, rect);

				for (int i = 0; i < bitSampleGridSize; i++) {
					// sample the inner region to avoid edge conditions on the boundary of white/black
					bitSquare.y = (rect.p1.y - rect.p0.y)*(padding + sampleLength*i/(bitSampleGridSize - 1)) + rect.p0.y;
					for (int j = 0; j < bitSampleGridSize; j++) {
						bitSquare.x = (rect.p1.x - rect.p0.x)*(padding + sampleLength*j/(bitSampleGridSize - 1)) + rect.p0.x;

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

	/**
	 * Given the markerID and cellID, compute the coordinate of the top left corner.
	 *
	 * @param markerID (Input) Marker
	 * @param cellID (Input) Encoded cell ID
	 * @param coordinate (Output) Corner coordinate in corner grid of TL corner
	 */
	public void cellIdToCornerCoordinate( int markerID, int cellID, GridCoordinate coordinate ) {
		GridShape grid = markers.get(markerID);

		// number of encoded squares in a two row set
		int setCount = grid.cols - 2;
		int setHalf = setCount/2;

		int squareRow = 1 + 2*(cellID/setCount) + (cellID%setCount < setHalf ? 0 : 1);
		int squareCol = squareRow%2 == 0 ? (cellID%setCount - setHalf)*2 + 1 : (cellID%setCount + 1)*2;

		coordinate.row = squareRow - 1;
		coordinate.col = squareCol - 1;
	}

	/**
	 * Rotates the observed coordinate system so that it aligns with the decoded coordinate system
	 */
	static void rotateObserved( int numRows, int numCols, int row, int col, int orientation, GridCoordinate found ) {
		switch (orientation) {
			case 0 -> found.setTo(row, col); // top-left
			case 1 -> found.setTo(-col, row); // top-right
			case 2 -> found.setTo(-row, -col); // bottom-right
			case 3 -> found.setTo(col, -row);  // bottom-left
			default -> throw new IllegalStateException("Unknown orientation");
		}
	}

	/**
	 * Adjust the top left corner based on orientation
	 */
	public static void adjustTopLeft( int orientation, GridCoordinate coordinate ) {
		switch (orientation) {
			case 0 -> {
			}
			case 1 -> coordinate.row -= 1;
			case 2 -> {
				coordinate.row -= 1;
				coordinate.col -= 1;
			}
			case 3 -> coordinate.col -= 1;
			default -> throw new IllegalArgumentException("Unknown orientation: " + orientation);
		}
	}

	/**
	 * Returns the number of encoded cells in the chessboard
	 *
	 * @param markerID (Input) which marker
	 */
	public int countEncodedSquaresInMarker( int markerID ) {
		GridShape grid = markers.get(markerID);
		int setCount = grid.cols - 2;

		int total = ((grid.rows - 2)/2)*setCount;
		if (grid.rows%2 == 1)
			total += (grid.cols - 1)/2;

		return total;
	}
}
