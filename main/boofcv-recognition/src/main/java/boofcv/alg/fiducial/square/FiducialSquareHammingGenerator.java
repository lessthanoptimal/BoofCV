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
import boofcv.factory.fiducial.ConfigHammingDictionary;
import boofcv.factory.fiducial.ConfigHammingDictionary.Marker;
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

	final ConfigHammingDictionary dictionary;

	// used to draw the fiducial
	@Setter protected FiducialRenderEngine render;

	public FiducialSquareHammingGenerator( ConfigHammingDictionary dictionary ) {
		this.dictionary = dictionary;
	}

	public void render( int markerIdx ) {
		Marker shape = dictionary.encoding.get(markerIdx);

		render.init();

		// Render black square surrounding the data bits
		renderBorder();

		// Render the unique ID for all inner squares
		renderCodes(shape);
	}

	private void renderBorder() {
		// Start drawing black
		render.setGray(0.0);

		double bw = dictionary.borderWidthFraction*squareWidth;

		// bottom and top
		render.rectangle(offsetX, offsetY, offsetX + squareWidth, offsetY + bw);
		render.rectangle(offsetX, offsetY + squareWidth - bw, offsetX + squareWidth, offsetY + squareWidth);

		// left and right
		render.rectangle(offsetX, offsetX + bw, offsetX + bw, offsetY + squareWidth - bw);
		render.rectangle(offsetX + squareWidth - bw, offsetY + bw, offsetX + squareWidth, offsetY + squareWidth - bw);
	}

	/**
	 * Renders unique IDs on all the inner squares
	 */
	private void renderCodes( Marker marker ) {
		final int rows = dictionary.gridWidth;
		final int cols = dictionary.gridWidth;

		render.setGray(0.0);

		// border width
		double bw = dictionary.borderWidthFraction*squareWidth;
		// square bit width
		double bit = (squareWidth - 2.0*bw)/dictionary.gridWidth;

		for (int row = 0, bitIndex = 0; row < rows; row++) {
			double y0 = offsetY + bw + (rows - row - 1)*bit;
			for (int col = 0; col < cols; col++) {
				double x0 = offsetX + bw + (cols - col - 1)*bit;
				if (marker.pattern.get(bitIndex++) == 1)
					continue;
				render.rectangle(x0, y0, x0 + bit, y0 + bit);
			}
		}
	}
}
