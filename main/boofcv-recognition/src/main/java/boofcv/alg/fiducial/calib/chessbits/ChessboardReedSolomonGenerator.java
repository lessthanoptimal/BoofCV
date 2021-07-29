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

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.struct.GridShape;
import georegression.struct.shapes.Rectangle2D_F64;

/**
 * Renders a chessboard pattern with numbers encoded inside the dots.
 *
 * TODO describe encoding
 *
 * @author Peter Abeles
 */
public class ChessboardReedSolomonGenerator {
	// Design Notes:
	// Encoding using both circles and squares were considered. Space was left between the shapes and they both ended
	// to excite the x-corner detector weakly, but enough for a corner to appear. Squares have the advantage that the
	// line connecting two false x-corners does not look like a chessboard line.
	//
	// Data can be embedded in the white or black squares inside the chessboard pattern. When put inside of the black
	// squares it reduces the effective range of the chessboard detection algorithm by a significant amount.

	// How wide a square is in the chessboard
	double squareWidth;

	final ChessBitsUtils utils;

	// used to draw the fiducial
	protected FiducialRenderEngine render;
	PackedBits8 packetBits = new PackedBits8();

	// Workspace
	Rectangle2D_F64 rect = new Rectangle2D_F64();

	public ChessboardReedSolomonGenerator( ChessBitsUtils utils ) {
		this.utils = utils;
	}

	public void render( int marker ) {
		GridShape shape = utils.markers.get(marker);

		render.init();

		// First build the chessboard pattern
		renderSquares(shape);

		// Render the unique ID for all inner squares
		renderCodes(shape);
	}

	private void renderSquares( GridShape shape ) {
		final int rows = shape.rows;
		final int cols = shape.cols;

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
	private void renderCodes( GridShape shape ) {
		final int rows = shape.rows;
		final int cols = shape.cols;

		// White circles will be rendered inside the inner squares
		render.setGray(0.0);

		int coordinateMultiplier = 1; // todo update

		double stub = squareWidth/2;
		for (int col = 1; col < cols; col += 1) {
			boolean borderCol = col == cols - 1;
			double x = stub + squareWidth*(col - 1);
			if ((col - 1)%coordinateMultiplier != 0)
				continue;
			for (int row = (col + 1)%2; row < rows; row += 2) {
				if ((row - 1)%coordinateMultiplier != 0)
					continue;

				boolean borderRow = row == 0 || row == rows - 1;
				double y = row == 0 ? 0.0 : stub + squareWidth*(row - 1);

				if (borderCol || borderRow)
					continue;

				// Encode the coordinate into bits
				utils.codec.encode(row - 1, col - 1, packetBits);
				renderEncoding(x, y);
			}
		}
	}

	/**
	 * Renders the specified value into a black square with white dots
	 */
	private void renderEncoding( double px, double py ) {
		int dotGridSize = utils.codec.getGridBitLength();

		for (int col = 0; col < dotGridSize; col++) {
			for (int row = 0; row < dotGridSize; row++) {
				int bit = row*dotGridSize + col;
				if (packetBits.get(bit) == 0)
					continue;

				// Get bit location in bit-grid coordinates
				utils.bitRect(row, col, rect);

				// Render it after scaling it into pixels
				render.square(px + rect.p0.x*squareWidth, py + rect.p0.y*squareWidth, squareWidth*rect.getWidth());
			}
		}
	}
}
