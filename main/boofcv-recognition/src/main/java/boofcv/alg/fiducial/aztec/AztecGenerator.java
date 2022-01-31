/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I32;

/**
 * Generates an image of an Aztec Code marker as specified in ISO/IEC 24778:2008(E)
 *
 * @author Peter Abeles
 */
public class AztecGenerator {
	/** How wide the square which encloses the marker is */
	public @Getter double markerWidth = 1.0;

	// Derived constants
	protected int lengthInSquares; // length of a side on the marker in units of squares
	protected double squareWidth; // length of a square in document units
	protected int orientationSquareCount; // squares in orientation region
	protected double orientationLoc; // (x,y) image coordinate of top-left corner of orientation pattern square
	/** Used to render the marker */
	@Getter protected FiducialRenderEngine render;

	/** Location of each bit in marker square coordinates */
	protected DogArray_I16 dataCoordinates = new DogArray_I16();
	protected DogArray_I32 layerStartsAtBit = new DogArray_I32();

	AztecEncoder encoder = new AztecEncoder();
	PackedBits8 bits = new PackedBits8();

	/** Convenience function for rendering images */
	public static GrayU8 renderImage( int pixelPerSquare, int border, AztecCode marker ) {
		int numSquares = marker.getMarkerSquareCount();
		var render = new FiducialImageEngine();
		render.configure(pixelPerSquare*border, numSquares*pixelPerSquare);
		new AztecGenerator().setMarkerWidth(numSquares*pixelPerSquare).setRender(render).render(marker);
		return render.getGray();
	}

	public AztecGenerator render( AztecCode marker ) {
		lengthInSquares = marker.getMarkerSquareCount();
		squareWidth = markerWidth/lengthInSquares;

		render.init();

		// Render the orientation and locator patterns
		orientationSquareCount = marker.getLocatorSquareCount() + 4;
		//noinspection IntegerDivisionInFloatingPointContext
		orientationLoc = ((lengthInSquares - orientationSquareCount)/2)*squareWidth;

		renderFixedPatterns(marker);
		renderModeMessage(marker);
		renderDataLayers(marker);

		return this;
	}

	/**
	 * Renders symbols which are fixed because they are independent of the encoded data
	 */
	private void renderFixedPatterns( AztecCode marker ) {
//		orientationPattern(orientationLoc, orientationLoc, orientationSquareCount);

		locatorPattern(orientationLoc + 2*squareWidth, orientationLoc + 2*squareWidth,
				marker.getLocatorRingCount(), marker.locatorRings);

		// Render the reference grid
		if (marker.structure == AztecCode.Structure.FULL && marker.dataLayers > 4) {
			int center = lengthInSquares/2;
			int odd = ((lengthInSquares - orientationSquareCount)/2)%2;
			for (int location = 0; center + location < lengthInSquares; location += 16) {
				referenceGridLine(odd, center - location, 1, 0, lengthInSquares);
				referenceGridLine(odd, center + location, 1, 0, lengthInSquares);

				referenceGridLine(center - location, odd, 0, 1, lengthInSquares);
				referenceGridLine(center + location, odd, 0, 1, lengthInSquares);
			}
		}
	}

	/**
	 * First encodes the mode then renders the mode around the orientation pattern
	 */
	private void renderModeMessage( AztecCode marker ) {
		// Encode the mode into a binary message
		encoder.encodeModeMessage(marker, bits);

		// short hand variables to cut down on verbosity
		final double s = squareWidth;
		// width of orientation pattern
		final double w = orientationSquareCount*s;

		// data is not rendered inside the reference grid
		boolean hasGrid = marker.structure == AztecCode.Structure.FULL;

		// Number of bits along each side of the orientation pattern
		int n = orientationSquareCount - 2 - (hasGrid ? 1 : 0);

		// Render the mode message along each side
		encodeBitsLine(0, n, orientationLoc + s, orientationLoc - s, 1, 0, hasGrid);
		encodeBitsLine(n, n, orientationLoc + w, orientationLoc + s, 0, 1, hasGrid);
		encodeBitsLine(n*2, n, orientationLoc + w - 2*s, orientationLoc + w, -1, 0, hasGrid);
		encodeBitsLine(n*3, n, orientationLoc - s, orientationLoc + w - 2*s, 0, -1, hasGrid);
	}

	/** Renders the encoded message while skipping over reference grid */
	private void renderDataLayers( AztecCode marker ) {
		// Get the location of each bit in the image
		computeDataBitCoordinates(marker, dataCoordinates, layerStartsAtBit);

		// Draw the bits which have a value of one
		for (int i = 0; i < dataCoordinates.size; i += 2) {
			int row = dataCoordinates.data[i];
			int col = dataCoordinates.data[i + 1];

			int x = col + lengthInSquares/2;
			int y = row + lengthInSquares/2;

			render.square(x*squareWidth, y*squareWidth, squareWidth);
		}
	}

	/**
	 * Draws binary data long a line. If skipMiddle is true then it will skip over the square
	 * in the middle of the line. All bits are encoded.
	 *
	 * (x0, y0) = initial coordinate in the image
	 * (dx, dy) = indicate the direction in units of squares
	 */
	void encodeBitsLine( int startBit, int count,
						 double x0, double y0, int dx, int dy,
						 boolean skipMiddle ) {

		if (skipMiddle) {
			// Render the first half
			int middle = count/2;
			encodeBitsLine(startBit, middle, x0, y0, dx, dy);
			// start of the second half of the bit
			int startBit2 = startBit + middle;
			// render the remainder of the bits, skipping over the middle
			x0 += dx*(1 + middle)*squareWidth;
			y0 += dy*(1 + middle)*squareWidth;
			encodeBitsLine(startBit2, count - middle, x0, y0, dx, dy);
		} else {
			encodeBitsLine(startBit, count, x0, y0, dx, dy);
		}
	}

	/** Renders bits along the specified line */
	void encodeBitsLine( int startBit, int count, double x0, double y0, int dx, int dy ) {
		for (int bit = 0; bit < count; bit++) {
			// 0 is encoded as white, which is the background color
			if (bits.get(startBit + bit) == 0)
				continue;

			// Draw the square
			render.square(x0 + dx*bit*squareWidth, y0 + dy*bit*squareWidth, squareWidth);
		}
	}

	/**
	 * Renders the locator pattern given it's top-left corner of the outermost ring. This does NOT include
	 * the orientation pattern.
	 */
	protected void locatorPattern( double tl_x, double tl_y, int ringCount, DogArray<Polygon2D_F64> ringLocations ) {
		ringLocations.resetResize(ringCount);
		for (int ring = ringCount; ring > 0; ring--) {
			// number of squares wide the ring is
			int modules = (ring - 1)*4 + 1;
			// Number of document units wide the ring is
			double width = squareWidth*modules;

			// Draw the ring
			render.square(tl_x, tl_y, width, squareWidth);

			// Save the ring's outer contour
			Polygon2D_F64 where = ringLocations.get(ring - 1);
			where.get(0).setTo(tl_x, tl_y);
			where.get(1).setTo(tl_x + width, tl_y);
			where.get(2).setTo(tl_x + width, tl_y + width);
			where.get(3).setTo(tl_x, tl_y + width);

			tl_x += 2*squareWidth;
			tl_y += 2*squareWidth;
		}
	}

	protected void orientationPattern( double tl_x, double tl_y, int moduleCount ) {
		// shorthand
		final double sw = squareWidth;
		final double rw = moduleCount*sw;

		// render the big square ring
		render.square(tl_x, tl_y, rw, sw);

		// top-left
		render.square(tl_x - sw, tl_y, sw);
		render.rectangleWH(tl_x - sw, tl_y - sw, 2*sw, sw);

		// top-right
		render.rectangleWH(tl_x + rw, tl_y - sw, sw, 2*sw);

		// bottom-right
		render.square(tl_x + rw, tl_y + rw - sw, sw);
	}

	protected void referenceGridLine( int tl_x, int tl_y, int dx, int dy, int count ) {
		int forbidden0 = (lengthInSquares - orientationSquareCount)/2 - 1;
		int forbidden1 = forbidden0 + orientationSquareCount + 2;

		for (int i = 0; i < count; i += 2) {
			int squareX = tl_x + i*dx;
			int squareY = tl_y + i*dy;

			// Don't draw inside the locator and orientation patterns
			if (squareX >= forbidden0 && squareX < forbidden1 && squareY >= forbidden0 && squareY < forbidden1)
				continue;

			double x = squareX*squareWidth;
			double y = squareY*squareWidth;

			render.square(x, y, squareWidth, squareWidth);
		}
	}

	/**
	 * Computes the coordinate of each bit in the marker in units of squares as specified in Figure 5.
	 * Coordinate system will have (0, 0) be the marker's center. +x right and +y down.
	 *
	 * @param coordinates Coordinates are encoded as interleaved (row, col)
	 * @param layerStart The bit at which each layer starts
	 */
	public static void computeDataBitCoordinates( AztecCode marker,
												  DogArray_I16 coordinates, DogArray_I32 layerStart ) {
		// clear old data
		coordinates.reset();
		layerStart.reset();

		// First layer goes around the orientation pattern + mode bits
		int ringWidth = marker.getLocatorSquareCount() + 6;
		int ringRadius = ringWidth/2;

		// is there a reference grid?
		boolean hasGrid = marker.dataLayers > 4;

		// initial coordinates for traversal
		int row = -ringRadius - 2;
		int col = -ringRadius;
		int maxNum = ringWidth + 2 - (hasGrid ? 1 : 0);
		int cornerJump = 2; // number of squares it needs to move to get to the next start corner

		// traverse the marker in a spiral pattern starting from the innermost layer outside the static objects
		for (int layer = 1; layer <= marker.dataLayers; layer++) {
			layerStart.add(coordinates.size);
			// Add coordinates one side at a time in this layer
			int traversed = dataBitCoordinatesLine(row, col, 0, 1, maxNum, hasGrid, coordinates);
			col += traversed - 1;
			row += cornerJump;
			dataBitCoordinatesLine(row, col, 1, 0, maxNum, hasGrid, coordinates);
			col -= cornerJump;
			row += traversed - 1;
			dataBitCoordinatesLine(row, col, 0, -1, maxNum, hasGrid, coordinates);
			col -= traversed - 1;
			row -= cornerJump;
			dataBitCoordinatesLine(row, col, -1, 0, maxNum, hasGrid, coordinates);

			// Move the corner to the next Row
			row -= traversed + 1;
			maxNum = maxNum + 4;

			// See if the next layer will encounter a reference grid that needs to be jumped over
			if (row%16 == 0 || (row + 1)%16 == 0) {
				row -= 1;
				cornerJump = 3;
			} else {
				cornerJump = 2;
			}
		}
	}

	/**
	 * Adds "domino" coordinates long the line while taking in account the reference grid
	 *
	 * @param row0 initial grid coordinate
	 * @param drow direction it should scan along
	 * @param length number of squares along the line it needs to write
	 * @param hasGrid If the marker has a reference grid
	 * @return Returns distance it traversed, including skipped squares
	 */
	static int dataBitCoordinatesLine( int row0, int col0, int drow, int dcol, int length,
									   boolean hasGrid,
									   DogArray_I16 coordinates ) {
		// See if the other side of the domino will hit a reference grid and needs to jump over it
		int leg = 1;
		if (hasGrid && (((row0 + dcol)%16) == 0 || ((col0 - drow)%16) == 0))
			leg = 2;

		// Add dominoes along the line while skipping over reference grids
		coordinates.reserve(coordinates.size + 4*length);
		int locationIndex = coordinates.size;
		int traversed = 0;
		int written = 0;
		while (written < length) {
			int row = row0 + drow*traversed;
			int col = col0 + dcol*traversed;
			traversed++;

			// see if this coordinate is touching a reference grid
			if (hasGrid && ((row%16) == 0 || (col%16) == 0))
				continue;

			// "domino" with most significant bit first. Makes sense if you look at figure 5
			coordinates.data[locationIndex++] = (short)row;
			coordinates.data[locationIndex++] = (short)col;
			coordinates.data[locationIndex++] = (short)(row + dcol*leg);
			coordinates.data[locationIndex++] = (short)(col - drow*leg);
			coordinates.size += 4;

			written++;
		}
		return traversed;
	}

	public AztecGenerator setMarkerWidth( double width ) {
		this.markerWidth = width;
		return this;
	}

	public AztecGenerator setRender( FiducialRenderEngine render ) {
		this.render = render;
		return this;
	}
}

