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

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.struct.GridShape;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an Error COrrecting Checkerboard (ECoCheck) marker to an image. The marker is composed of a checkerboard
 * pattern combined with binary encoding in white spaces between the black checkerboard pattern. Using the encoded
 * binary pattern, its possible to uniquely determine the marker, and which calibration points are being observed
 * even when parts of the marker go outside the image bounds or are obstructed.
 *
 * TODO Add image of an example.
 *
 * A standard checkerboard pattern is drawn first. The outer squares will be half width as to maximize the usable
 * space. Only inner corners (x-corners) are used as calibration points and a full outer square does not aide
 * significantly in detection. If they were too small then the ability to detect x-corners would degrade. The
 * square grid is defined using the black and white squares with the origin being the top-left square, which
 * is always black, +x is to the right and +y is down. A quite-zone of white is required around the marker
 * to reduce confusion with the background. Omitting the quite-zone will degrade performance in challenging
 * environments.
 *
 * Inner white squares are encoded with a binary pattern that specifies which square its inside of and which marker
 * its a member of. Some bits are allocated towards a checksum and others can perform error correction. A complete
 * description of the encoding can be found inside of {@link ECoCheckCodec} and how the image processing is done in
 * {@link ECoCheckDetector}.
 *
 * TODO define the order in which bits are drawn and where the origin is. Also define the corner-grid coordinate system.
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
@SuppressWarnings({"NullAway.Init"})
public class ECoCheckGenerator {
	// Design Notes:
	// Encoding using both circles and squares were considered. Space was left between the shapes and they both ended
	// to excite the x-corner detector weakly, but enough for a corner to appear. Squares have the advantage that the
	// line connecting two false x-corners does not look like a chessboard line.
	//
	// Data can be embedded in the white or black squares inside the chessboard pattern. When put inside of the black
	// squares it reduces the effective range of the chessboard detection algorithm by a significant amount.

	/** How wide a checkerboard square is */
	public @Setter @Getter double squareWidth = 1.0;

	final ECoCheckUtils utils;

	// used to draw the fiducial
	@Setter protected FiducialRenderEngine render;
	PackedBits8 packetBits = new PackedBits8();

	// Workspace
	Rectangle2D_F64 rect = new Rectangle2D_F64();

	// list of corners in ground truth
	public final List<Point2D_F64> corners = new ArrayList<>();

	public ECoCheckGenerator( ECoCheckUtils utils ) {
		this.utils = utils;
		utils.checkFixate();
	}

	public void render( int marker ) {
		corners.clear();
		GridShape shape = utils.markers.get(marker);

		render.init();

		// First build the chessboard pattern
		renderSquares(shape);

		// Render the unique ID for all inner squares
		renderCodes(marker, shape);

		// save ground truth corner location
		saveCornerLocations(shape);
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
	private void renderCodes( int marker, GridShape shape ) {
		final int rows = shape.rows;
		final int cols = shape.cols;

		// White circles will be rendered inside the inner squares
		render.setGray(0.0);

		double stub = squareWidth/2;
		int cellID = 0;
		for (int row = 1; row < rows; row++) {
			boolean borderRow = row == rows - 1;
			double y = stub + squareWidth*(row - 1);
			for (int col = 1 + row%2; col < cols; col += 2) {
				boolean borderCol = col == cols - 1;
				double x = stub + squareWidth*(col - 1);

				if (borderCol || borderRow)
					continue;

				// Encode the coordinate into bits
				utils.codec.encode(marker, cellID++, packetBits);
				renderEncoding(x, y);
			}
		}
	}

	public void saveCornerLocations( GridShape shape ) {
		corners.clear();
		final int rows = shape.rows;
		final int cols = shape.cols;

		double stub = squareWidth/2;
		for (int row = 1; row < rows; row++) {
			double y = stub + squareWidth*(row - 1);
			for (int col = 1; col < cols; col++) {
				double x = stub + squareWidth*(col - 1);

				corners.add(new Point2D_F64(x, y));
			}
		}
	}

	/**
	 * Renders the specified value into a black square with white dots
	 */
	private void renderEncoding( double px, double py ) {
		int dotGridSize = utils.codec.getGridBitLength();

		for (int row = 0; row < dotGridSize; row++) {
			for (int col = 0; col < dotGridSize; col++) {
				int bit = utils.bitOrder.get(row*dotGridSize + col);
				if (packetBits.get(bit) == 0)
					continue;

				// Get bit location in bit-grid coordinates
				utils.bitRect(row, col, rect);

				// Render it after scaling it into pixels
				render.square(px + rect.p0.x*squareWidth, py + rect.p0.y*squareWidth, rect.getWidth()*squareWidth);
			}
		}
	}
}
