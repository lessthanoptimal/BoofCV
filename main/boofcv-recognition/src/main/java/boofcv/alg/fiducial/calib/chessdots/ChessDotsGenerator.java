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

/**
 * Renders a chessboard pattern with numbers encoded inside the dots.
 *
 * TODO describe encoding
 *
 * @author Peter Abeles
 */
public class ChessDotsGenerator {
	// TODO reduce size of outside squares
	// TODO real encoding
	// TODO Render a panel ID into the center of a middle white block?
	// How wide a square is in the chessboard
	double squareWidth;
	// Number of columns in the chessboard pattern
	int cols;
	// Number of rows in the chessboard pattern
	int rows;
	// Number of dots wide the encoded grid is
	int dotGridSize = 5;
	// Fraction of a cell a dot is
	double circleWidthFraction = 0.7;

	// used to draw the fiducial
	protected FiducialRenderEngine render;

	public void render( int rows, int cols ) {
		this.rows = rows;
		this.cols = cols;

		render.init();

		// Start drawing black
		render.setGray(0.0);

		// First build the chessboard pattern
		for (int col = 0; col < cols; col++) {
			double x = squareWidth*col;
			for (int row = col%2; row < rows; row += 2) {
				double y = squareWidth*row;
				render.square(x, y, squareWidth, squareWidth);
			}
		}

		// Render the unique ID for all inner squares
		renderCodes();
	}

	/**
	 * Renders unique IDs on all the inner squares
	 */
	private void renderCodes() {
		// White circles will be rendered inside the inner squares
		render.setGray(1.0);

		int count = 0;
		for (int col = 1; col < cols - 1; col++) {
			double x = squareWidth*col;
			for (int row = col%2; row < rows; row += 2) {
				double y = squareWidth*row;
				renderEncoding(x, y, count++);
			}
		}
	}

	/**
	 * Renders the specified value into a black square with white dots
	 */
	private void renderEncoding( double px, double py, int value ) {
		double cellWidth = squareWidth/(2 + dotGridSize);
		double circleRadius = cellWidth*0.5*circleWidthFraction;

		for (int col = 0; col < dotGridSize; col++) {
			double x = px + (col + 1)*cellWidth;
			for (int row = 0; row < dotGridSize; row++) {
				double y = py + (row + 1)*cellWidth;

				render.circle(x + circleRadius, y + circleRadius, circleRadius);
			}
		}
	}
}
