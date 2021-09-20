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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.struct.GridCoordinate;
import boofcv.struct.GridShape;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * <p>Common functions that are needed for encoding, decoding, and detecting. Terminology can be found in
 * {@link ECoCheckGenerator}.</p>
 *
 * @author Peter Abeles
 */
public class ECoCheckUtils {
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
	public final ECoCheckCodec codec = new ECoCheckCodec();

	/** How bits are ordered. This is done to make bits which are in the same  word spatially close to each other */
	public final DogArray_I32 bitOrder = new DogArray_I32();

	// Used to compute the homography from square coordinates into image pixels
	HomographyDirectLinearTransform dlt = new HomographyDirectLinearTransform(true);
	DogArray<AssociatedPair> storagePairs2D = new DogArray<>(AssociatedPair::new);
	// homography that describes the transform from data-region (0 to 1.0) to image pixels
	DMatrixRMaj squareToPixel = new DMatrixRMaj(3, 3);

	// pre-allcoated workspace
	Rectangle2D_F64 rect = new Rectangle2D_F64();
	Point2D_F64 bitSquare = new Point2D_F64();

	// histogram storage for otsu threshold
	DogArray_I32 histogram = new DogArray_I32();

	{
		histogram.resize(100);
	}

	/**
	 * Assign all parameters from a config class as possible
	 */
	public void setParametersFromConfig( ConfigECoCheckMarkers config ) {
		this.dataBitWidthFraction = config.dataBitWidthFraction;
		this.dataBorderFraction = config.dataBorderFraction;
		this.codec.setChecksumBitCount(config.checksumBits);
		this.codec.setErrorCorrectionLevel(config.errorCorrectionLevel);
		config.convertToGridList(markers);
	}

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
		codec.configure(markers.size(), findMaxEncodedSquares());

		bitSampleCount = bitSampleGridSize*2 + 1;
		new ECoCheckLayout().selectSnake(codec.gridBitLength, bitOrder);
	}

	public void checkFixate() {
		if (bitSampleCount == 0)
			throw new RuntimeException("BUG you forgot to call fixate()");
	}

	/**
	 * Returns the max number of encoded squares found in any of the markers
	 */
	int findMaxEncodedSquares() {
		int largest = 0;
		for (int i = 0; i < markers.size(); i++) {
			GridShape g = markers.get(i);

			int rows = g.rows - 2;
			int cols = g.cols - 2;

			int count = (rows/2)*cols + (rows%2)*(cols/2);
			if (count > largest) {
				largest = count;
			}
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
	 * Computes a threshold using Otsu. Assumes that there are two clear peaks in the data.
	 */
	public float otsuThreshold( DogArray_F32 values ) {
		// Find the local range
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		for (int i = 0; i < values.size; i++) {
			min = Math.min(min, values.data[i]);
			max = Math.max(max, values.data[i]);
		}
		float range = max - min;

		// Use range to convert into a discrete histogram
		histogram.fill(0);

		for (int i = 0; i < values.size; i++) {
			float v = values.data[i];
			int index = (int)(histogram.size*(v - min)/range);
			histogram.data[Math.min(histogram.size - 1, index)]++;
		}

		// Select threshold using OTSU
		int selected = GThresholdImageOps.computeOtsu(histogram.data, histogram.size, values.size);

		// Convert back into a float value
		return range*selected/(histogram.size - 1) + min;
	}

	/**
	 * Convenience function to go from a point in grid coordinates to image pixels
	 */
	public void gridToPixel( double x, double y, Point2D_F64 pixel ) {
		bitSquare.setTo(x, y);
		GeometryMath_F64.mult(squareToPixel, bitSquare, pixel);
	}

	/**
	 * Selects pixels that it should sample for each bit. The number of pixels per bit is specified by pixelsPerBit.
	 * The order of bits is in row-major format with a block of size bitSampleCount. Points are sampled in a grid
	 * pattern with one point in the center. This means there will be an odd number of points preventing a tie.
	 *
	 * @param pixels (Output) Image pixels that correspond pixels in binary version of grid
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

	/**
	 * Converts a corner ID into a marker ID. The origin of the marker's coordinate system will be the marker's center.
	 * Coordinates will range from -0.5 to 0.5. The length of the longest side will be 1.0.
	 *
	 * @param markerID Which marker
	 * @param cornerID Which corner
	 * @param coordinate (Output) Coordinate in the coordinate system described above
	 */
	public void cornerToMarker3D( int markerID, int cornerID, Point3D_F64 coordinate ) {
		GridShape grid = markers.get(markerID);
		double squareToUnit = 1.0/(Math.max(grid.cols, grid.rows) - 1);

		cornerToMarker3D(markerID, cornerID, squareToUnit, coordinate);
	}

	public void cornerToMarker3D( int markerID, int cornerID, double squareLength, Point3D_F64 coordinate ) {
		GridShape grid = markers.get(markerID);

		// size of square grid
		double width = (grid.cols - 1)*squareLength;
		double height = (grid.rows - 1)*squareLength;

		int row = cornerID/(grid.cols - 1);
		int col = cornerID%(grid.cols - 1);

		coordinate.x = (0.5 + col)*squareLength - width/2.0;
		coordinate.y = (0.5 + row)*squareLength - height/2.0;
		coordinate.z = 0;

		// normally +y is up and not down like in images
		coordinate.y *= -1.0;
	}

	/**
	 * Creates a list of corners coordinates on the specified marker given the size of a full square
	 *
	 * @param markerID Which marker
	 * @param squareLength Size of a full square
	 * @return Location of corners in standard order
	 */
	public List<Point2D_F64> createCornerList( int markerID, double squareLength ) {
		List<Point2D_F64> corners = new ArrayList<>();

		GridShape grid = markers.get(markerID);

		// size of square grid
		double width = (grid.cols - 1)*squareLength;
		double height = (grid.rows - 1)*squareLength;

		int numCorners = (grid.rows - 1)*(grid.cols - 1);

		for (int cornerID = 0; cornerID < numCorners; cornerID++) {
			int row = cornerID/(grid.cols - 1);
			int col = cornerID%(grid.cols - 1);

			Point2D_F64 coordinate = new Point2D_F64();
			coordinate.x = (0.5 + col)*squareLength - width/2.0;
			coordinate.y = (0.5 + row)*squareLength - height/2.0;

			// normally +y is up and not down like in images
			coordinate.y *= -1.0;

			corners.add(coordinate);
		}

		return corners;
	}

	public static int maxCorners( int rows, int cols ) {
		return (rows - 1)*(cols - 1);
	}

	public boolean isLegalCornerIds( ECoCheckFound found ) {
		GridShape shape = markers.get(found.markerID);
		int maxCornerID = maxCorners(shape.rows, shape.cols);
		for (int cornerIdx = 0; cornerIdx < found.corners.size; cornerIdx++) {
			if (found.corners.get(cornerIdx).index >= maxCornerID) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Merges detections with the same marker ID into a single detection and removes unknown markers. A naive
	 * algorithm is used to merge markers together. First it checks to see if there's a conflict between two markers
	 * by them having the same cornerID. If that's good it will select the existing marker with the most corners.
	 * If there is no matching marker then a new marker is created.
	 *
	 * @param found Original list of found markers.
	 * @return New list of markers.
	 */
	public List<ECoCheckFound> mergeAndRemoveUnknown( List<ECoCheckFound> found ) {
		List<ECoCheckFound> merged = new ArrayList<>();
		DogArray_B used = new DogArray_B();

		for (int foundIdx = 0; foundIdx < found.size(); foundIdx++) {
			ECoCheckFound f = found.get(foundIdx);

			// Skip unknown markers
			if (f.markerID < 0)
				continue;
			// Sanity check corners
			if (!isLegalCornerIds(f))
				continue;

			// Make sure this is a valid pattern
			GridShape shape = markers.get(f.markerID);
			used.resetResize(maxCorners(shape.rows, shape.cols), false);

			ECoCheckFound match = null;
			for (int mergedIdx = 0; mergedIdx < merged.size(); mergedIdx++) {
				ECoCheckFound m = merged.get(mergedIdx);
				if (f.markerID != m.markerID)
					continue;

				// See if there's a conflict by having the same corner in both sets
				boolean conflict = false;
				for (int cornerIdx = 0; cornerIdx < m.corners.size; cornerIdx++) {
					used.data[m.corners.get(cornerIdx).index] = true;
				}
				for (int cornerIdx = 0; cornerIdx < f.corners.size; cornerIdx++) {
					if (used.data[f.corners.get(cornerIdx).index]) {
						conflict = true;
						break;
					}
				}

				if (conflict) {
					continue;
				}

				// Prefer larger patterns if there are multiple. Harder to have a false positive
				if (match == null || match.corners.size < m.corners.size) {
					match = m;
				}
			}

			if (match == null) {
				// create a copy so that the input isn't modified
				merged.add(new ECoCheckFound(f));
				continue;
			}

			// merge into a single result
			match.decodedCells.addAll(f.decodedCells);
			match.touchBinary.addAll(f.touchBinary);
			for (int j = 0; j < f.corners.size; j++) {
				match.corners.grow().setTo(f.corners.get(j));
			}
		}

		// Sort so that the largest is first
		Collections.sort(merged, Comparator.comparingInt(a -> -a.corners.size));

		// If there are duplicates only keep the largest
		for (int i = 0; i < merged.size(); i++) {
			int target = merged.get(i).markerID;
			for (int j = merged.size() - 1; j >= i + 1; j--) {
				if (merged.get(j).markerID == target) {
					merged.remove(j);
				}
			}
		}

		return merged;
	}
}
