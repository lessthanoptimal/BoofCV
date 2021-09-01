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

package boofcv.alg.fiducial.square;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.ConfigHammingMarker.Marker;
import lombok.Getter;
import lombok.Setter;

/**
 * Renders a square hamming fiducial. Bits with a value of 0 are white and bits with a value of 1 are black.
 *
 * @author Peter Abeles
 */
public class FiducialSquareHammingGenerator {
	/** How wide a checkerboard square is */
	public @Setter @Getter double squareWidth = 1.0;

	/** The top-left corner of the marker */
	public double offsetX, offsetY;

	final ConfigHammingMarker dictionary;

	// used to draw the fiducial
	@Setter protected FiducialRenderEngine render;

	public FiducialSquareHammingGenerator( ConfigHammingMarker dictionary ) {
		this.dictionary = dictionary;
	}

	public void render( int markerIdx ) {
		render.init();
		renderNoInit(markerIdx);

	}

	public void renderNoInit( int markerIdx ) {
		Marker shape = dictionary.encoding.get(markerIdx);

		// Render black square surrounding the data bits
		renderBorder();

		// Render the unique ID for all inner squares
		renderCodes(shape);
	}

	private void renderBorder() {
		// Start drawing black
		render.setGray(0.0);

		double bw = dictionary.borderWidthFraction*squareWidth;
		render.square(offsetX, offsetY, squareWidth, bw);
	}

	/**
	 * Renders unique IDs on all the inner squares
	 */
	public void renderCodes( Marker marker ) {
		final int rows = dictionary.gridWidth;
		final int cols = dictionary.gridWidth;

		render.setGray(0.0);

		// border width
		double bw = dictionary.borderWidthFraction*squareWidth;
		// square bit width
		double bit = (squareWidth - 2.0*bw)/dictionary.gridWidth;

		for (int row = 0, bitIndex = 0; row < rows; row++) {
			double y0 = offsetY + bw + (rows - row - 1)*bit;
			double y1 = offsetY + bw + (rows - row)*bit;
			for (int col = 0; col < cols; col++) {
				double x0 = offsetX + bw + (cols - col - 1)*bit;
				double x1 = offsetX + bw + (cols - col)*bit;
				if (marker.pattern.get(bitIndex++) == 1)
					continue;
				render.rectangle(x0, y0, x1, y1);
			}
		}
	}
}
