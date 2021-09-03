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

import boofcv.alg.drawing.FiducialImageGenerator;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.ConfigHammingMarker.Marker;
import lombok.Getter;

/**
 * Renders a square hamming fiducial. Bits with a value of 0 are white and bits with a value of 1 are black.
 *
 * @author Peter Abeles
 */
public class FiducialSquareHammingGenerator extends FiducialImageGenerator {
	/** The top-left corner of the marker */
	public double offsetX, offsetY;

	@Getter final ConfigHammingMarker config;

	public FiducialSquareHammingGenerator( ConfigHammingMarker config ) {
		this.config = config;
	}

	public void generate( int markerIdx ) {
		renderer.init();
		generateNoInit(markerIdx);
	}

	public void generateNoInit( int markerIdx ) {
		Marker shape = config.encoding.get(markerIdx);

		// Render black square surrounding the data bits
		renderBorder();

		// Render the unique ID for all inner squares
		renderCodes(shape);
	}

	private void renderBorder() {
		// Start drawing black
		renderer.setGray(0.0);

		double bw = config.borderWidthFraction*markerWidth;
		renderer.square(offsetX, offsetY, markerWidth, bw);
	}

	/**
	 * Renders unique IDs on all the inner squares
	 */
	public void renderCodes( Marker marker ) {
		final int rows = config.gridWidth;
		final int cols = config.gridWidth;

		renderer.setGray(0.0);

		// border width
		double bw = config.borderWidthFraction*markerWidth;
		// square bit width
		double bit = (markerWidth - 2.0*bw)/config.gridWidth;

		for (int row = 0, bitIndex = 0; row < rows; row++) {
			double y0 = offsetY + bw + (rows - row - 1)*bit;
			double y1 = offsetY + bw + (rows - row)*bit;
			for (int col = 0; col < cols; col++) {
				double x0 = offsetX + bw + (cols - col - 1)*bit;
				double x1 = offsetX + bw + (cols - col)*bit;
				if (marker.pattern.get(bitIndex++) == 1)
					continue;
				renderer.rectangle(x0, y0, x1, y1);
			}
		}
	}
}
