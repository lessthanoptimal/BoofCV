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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.drawing.FiducialRenderEngine;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Generates an image of a Micro QR Code.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeGenerator {
	/** How wide the square which encloses the marker is */
	public @Setter @Getter double markerWidth = 1.0;

	/** Used to render the marker */
	@Setter protected FiducialRenderEngine render;

	// how wide each bit is
	double moduleWidth;

	// number of modules wide a marker is
	int numModules;

	public MicroQrCodeGenerator render( MicroQrCode qr ) {
		numModules = MicroQrCode.totalModules(qr.version);
		moduleWidth = markerWidth/numModules;

		render.init();

		positionPattern(qr.pp);

		timingPattern(7*moduleWidth, 0, moduleWidth, 0);
		timingPattern(0, 7*moduleWidth, 0, moduleWidth);

		// TODO render format information

		// TODO render data bits

		qr.bounds.set(0, 0, 0);
		qr.bounds.set(1, markerWidth, 0);
		qr.bounds.set(2, markerWidth, markerWidth);
		qr.bounds.set(3, 0, markerWidth);

		return this;
	}

	private void positionPattern( Polygon2D_F64 where ) {
		// draw the outside square
		render.square(0.0, 0.0, moduleWidth*7, moduleWidth);

		// draw the inside square
		render.square(moduleWidth*2, moduleWidth*2, moduleWidth*3);

		where.get(0).setTo(0.0, 0.0);
		where.get(1).setTo(moduleWidth*7, 0.0);
		where.get(2).setTo(moduleWidth*7, moduleWidth*7);
		where.get(3).setTo(0.0, moduleWidth*7);
	}

	private void timingPattern( double x, double y, double slopeX, double slopeY ) {
		int length = numModules - 7;

		for (int i = 1; i < length; i += 2) {
			render.square(x + i*slopeX, y + i*slopeY, moduleWidth);
		}
	}
}
