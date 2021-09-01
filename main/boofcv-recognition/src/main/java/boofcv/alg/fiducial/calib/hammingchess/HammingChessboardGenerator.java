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

package boofcv.alg.fiducial.calib.hammingchess;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders Hamming markers inside of chessboard patterns similar to Charuco markers.
 *
 * @author Peter Abeles
 */
public class HammingChessboardGenerator {
	/** How wide a checkerboard square is */
	public @Setter @Getter double squareWidth = 1.0;

	// used to draw the fiducial
	@Setter protected FiducialRenderEngine render;

	final ConfigHammingChessboard config;

	// Used to render individual markers
	private final FiducialSquareHammingGenerator squareGenerator;

	// list of corners in ground truth
	public final List<Point2D_F64> corner = new ArrayList<>();

	public HammingChessboardGenerator( ConfigHammingChessboard config ) {
		this.config = config;
		this.squareGenerator = new FiducialSquareHammingGenerator(config.markers);
	}

	public void render() {
		render.init();
		render.setGray(0);
		squareGenerator.setRender(render);
		double w = squareWidth*config.squareSize;
		double markerOffset = w*(1.0 - config.markerScale)/2.0;
		squareGenerator.squareWidth = w*config.markerScale;

		int markerIndex = config.markerOffset;
		for (int row = 0; row < config.numRows; row++) {
			double y = row*w;
			for (int col = 0; col < config.numCols; col++) {
				double x = col*w;

				boolean drawSquare;
				if (config.chessboardEven) {
					drawSquare = col%2 == row%2;
				} else {
					drawSquare = col%2 != row%2;
				}

				if (drawSquare) {
					render.square(x, y, w);
				} else {
					squareGenerator.offsetX = x + markerOffset;
					squareGenerator.offsetY = y + markerOffset;
					squareGenerator.renderNoInit(markerIndex++);
				}
			}
		}

		saveCornerLocations();
	}

	public void saveCornerLocations() {
		corner.clear();

		final int rows = config.numRows;
		final int cols = config.numCols;
		final double w = squareWidth*config.squareSize;

		double stub = squareWidth/2;
		for (int row = 1; row < rows; row++) {
			double y = stub + w*(row - 1);
			for (int col = 1; col < cols; col++) {
				double x = stub + w*(col - 1);

				corner.add(new Point2D_F64(x, y));
			}
		}
	}
}
