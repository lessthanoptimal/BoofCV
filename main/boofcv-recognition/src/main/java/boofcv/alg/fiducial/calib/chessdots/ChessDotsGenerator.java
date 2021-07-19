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

package boofcv.alg.fiducial.calib.chessdots;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.qrcode.PackedBits8;

/**
 * Renders a chessboard pattern with numbers encoded inside the dots.
 *
 * TODO describe encoding
 *
 * @author Peter Abeles
 */
public class ChessDotsGenerator {
	// Design Notes:
	// Encoding using both circles and squares were considered. Space was left between the shapes and they both ended
	// to excite the x-corner detector weakly, but enough for a corner to appear. Squares have the advantage that the
	// line connecting two false x-corners does not look like a chessboard line.

	ChessboardSolomonMarkerCodec.Multiplier multiplier = ChessboardSolomonMarkerCodec.Multiplier.LEVEL_0;

	// TODO real encoding
	// TODO Render a panel ID into the center of a middle white block?
	// How wide a square is in the chessboard
	double squareWidth;
	/** Number of columns in the chessboard pattern */
	int cols;
	/** Number of rows in the chessboard pattern */
	int rows;

	/** Fraction of a cell's length the data bit is */
	double dataBitWidthFraction = 0.7;

	/** Fraction of the length the quite zone is around data bits */
	double dataBorderFraction = 0.15;

	// used to draw the fiducial
	protected FiducialRenderEngine render;
	PackedBits8 packetBits = new PackedBits8();

	ChessboardSolomonMarkerCodec codec = new ChessboardSolomonMarkerCodec();

	public void render( int rows, int cols ) {
		this.rows = rows;
		this.cols = cols;

		codec.configure(multiplier, Math.max(rows, cols));
		render.init();

		// First build the chessboard pattern
		renderSquares();

		// Render the unique ID for all inner squares
		renderCodes();
	}

	private void renderSquares() {
		// Start drawing black
		render.setGray(0.0);

		double stub = squareWidth/2;
		for (int col = 0; col < cols; col++) {
			boolean borderCol = col == 0 || col == cols - 1;
			double x = col == 0 ? 0.0 : stub + squareWidth*(col - 1);
			double lengthX = borderCol ? stub : squareWidth;
			for (int row = col%2; row < rows; row += 2) {
				boolean borderRow = row == 0 || row == rows - 1;
				double lengthY = borderRow ? stub : squareWidth;
				double y = row == 0 ? 0.0 : stub + squareWidth*(row - 1);
				render.rectangle(x, y, x + lengthX, y + lengthY);
			}
		}
	}

	/**
	 * Renders unique IDs on all the inner squares
	 */
	private void renderCodes() {
		// White circles will be rendered inside the inner squares
		render.setGray(1.0);

		int coordinateMultiplier = multiplier.getAmount();

		double stub = squareWidth/2;
		for (int col = 1; col < cols; col += 1) {
			boolean borderCol = col == cols - 1;
			double x = stub + squareWidth*(col - 1);
			if ((col - 1)%coordinateMultiplier != 0)
				continue;
			for (int row = col%2; row < rows; row += 2) {
				if ((row - 1)%coordinateMultiplier != 0)
					continue;

				boolean borderRow = row == 0 || row == rows - 1;
				double y = row == 0 ? 0.0 : stub + squareWidth*(row - 1);

				if (borderCol || borderRow)
					continue;

				// Encode the coordinate into bits
				codec.encode(row - 1, col - 1, packetBits);
				renderEncoding(x, y);
			}
		}
	}

	/**
	 * Renders the specified value into a black square with white dots
	 */
	private void renderEncoding( double px, double py ) {
		int dotGridSize = codec.getGridBitLength();
		double borderWidth = squareWidth*dataBorderFraction;
		double cellWidth = (squareWidth - borderWidth*2)/dotGridSize;
		double squareWidth = cellWidth*dataBitWidthFraction;

		// Account for the data shape being smaller than a cell
		double offset = borderWidth + cellWidth*(1.0 - dataBitWidthFraction)/2.0;

		for (int col = 0; col < dotGridSize; col++) {
			double x = px + col*cellWidth + offset;
			for (int row = 0; row < dotGridSize; row++) {
				double y = py + row*cellWidth + offset;

				int bit = row*dotGridSize + col;
				if (packetBits.get(bit) == 0)
					continue;
				render.square(x, y, squareWidth);
			}
		}
	}
}
