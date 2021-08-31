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
import lombok.Getter;
import lombok.Setter;

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

	public HammingChessboardGenerator( ConfigHammingChessboard config ) {
		this.config = config;
		this.squareGenerator = new FiducialSquareHammingGenerator(config.dictionary);
	}

	public void render() {
		double markerOffset = squareWidth*(1.0-config.markerScale)/2.0;
		squareGenerator.squareWidth = squareWidth*config.markerScale;

		int markerIndex = config.markerOffset;
		for (int row = 0; row < config.numRows; row++) {
			double y = row*squareWidth;
			for (int col = 0; col < config.numCols; col++) {
				double x = col*squareWidth;

				if (col%2==row%2) {
					render.square(x,y,squareWidth);
				} else {
					squareGenerator.offsetX = x + markerOffset;
					squareGenerator.offsetY = y + markerOffset;
					squareGenerator.render(markerIndex++);
				}
			}
		}
	}
}
